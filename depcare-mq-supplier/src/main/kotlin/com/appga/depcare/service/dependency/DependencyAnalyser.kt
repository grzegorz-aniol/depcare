package com.appga.depcare.service.dependency

import mu.KLogging
import com.appga.depcare.clients.MvnRepoClient
import com.appga.depcare.db.Repository
import com.appga.depcare.domain.VersionIndication
import com.appga.depcare.utils.forEach
import com.appga.depcare.utils.getFirstElement
import com.appga.depcare.utils.getFirstElementValue
import org.eclipse.microprofile.reactive.messaging.Channel
import org.eclipse.microprofile.reactive.messaging.Emitter
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.eclipse.microprofile.reactive.messaging.OnOverflow
import org.w3c.dom.Element
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject

@ApplicationScoped
class DependencyAnalyser(
	@Channel("out-deps")
	@OnOverflow(value = OnOverflow.Strategy.BUFFER, bufferSize = 10)
	private val payloadEmitter: Emitter<String>,
	@Inject
	private val mvnRepoClient: MvnRepoClient,
	@Inject
	private val repository: Repository
) {
	private companion object : KLogging()

	fun postPomLink(url: String) {
		payloadEmitter.send(url)
	}

	@Incoming("in-deps")
	protected fun consumer(url: String) {
		try {
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
