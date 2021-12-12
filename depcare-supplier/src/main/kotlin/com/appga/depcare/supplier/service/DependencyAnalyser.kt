package com.appga.depcare.supplier.service

import com.appga.depcare.domain.VersionIndication
import com.appga.depcare.supplier.clients.MvnRepoClient
import com.appga.depcare.supplier.configuration.KafkaTopics
import com.appga.depcare.supplier.db.Repository
import com.appga.depcare.supplier.utils.forEach
import com.appga.depcare.supplier.utils.getFirstElement
import com.appga.depcare.supplier.utils.getFirstElementValue
import mu.KLogging
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import org.w3c.dom.Element

@Service
class DependencyAnalyser(
	private val kafkaTemplate: KafkaTemplate<String, String>,
	private val mvnRepoClient: MvnRepoClient,
	private val repository: Repository
) {
	private companion object : KLogging()

	fun postPomLink(url: String) {
		kafkaTemplate.send(KafkaTopics.TOPIC_DEPS, url)
	}

	@KafkaListener(topics = [KafkaTopics.TOPIC_DEPS])
	protected fun consumer(url: String) {
		try {
			logger.info { "Dependency analysis for $url" }
			val doc = mvnRepoClient.fetchXmlDocument(url)
			val rootElement = doc.documentElement
			if (rootElement == null) {
				logger.error { "Missing root xml element" }
				return
			}
			rootElement.run {
				val parentVersionIndication = getFirstElement("parent")?.let { getVersionIndication(it) }
				val thisVersionIndication = getVersionIndication(this)
				val actualVersionIndication = mergeVersionIndications(parentVersionIndication, thisVersionIndication)
				if (!actualVersionIndication.isValid()) {
					logger.error { "Cannot find library group, artifact or version in POM xml. Url: $url" }
					return
				}
				if (parentVersionIndication != null && parentVersionIndication.isValid()) {
					repository.saveParentProject(actualVersionIndication, parentVersionIndication)
				}
				getFirstElement("dependencies")?.run {
					getElementsByTagName("dependency")?.forEach { dep ->
						val depIndication = getDependencyIndication(dep)
						if (depIndication.isValid()) {
							// Version may not be specified. In such case the right dependency version should be determined by analyzing the parent POM
							if (depIndication.hasVersion()) {
								repository.saveDependency(actualVersion = actualVersionIndication, dependency = depIndication)
							} else {
								repository.saveTransitiveDependency(actualVersion = actualVersionIndication, dependency = depIndication)
							}
						} else {
							logger.error { "Cannot find library dependency group, artifact or version ($url)" }
						}
					}
				}
			}
		} catch (e: Exception) {
			logger.error { "Error: ${e.message}. Url: $url" }
		}
	}

	private fun getVersionIndication(element: Element): VersionIndication {
		return VersionIndication(
			element.getFirstElementValue("groupId"),
			element.getFirstElementValue("artifactId"),
			element.getFirstElementValue("version"),
		)
	}

	private fun getDependencyIndication(element: Element): VersionIndication {
		return VersionIndication(
			element.getFirstElementValue("groupId"),
			element.getFirstElementValue("artifactId"),
			element.getFirstElementValue("version"),
			element.getFirstElementValue("scope"),
			element.getFirstElementValue("optional")?.let { it == "true" },
			element.getFirstElementValue("type"),
		)
	}

	private fun mergeVersionIndications(parent: VersionIndication?, current: VersionIndication?): VersionIndication {
		return VersionIndication(
			groupId = current?.groupId ?: parent?.groupId,
			artifactId = current?.artifactId,
			version = current?.version ?: parent?.version
		)
	}
}
