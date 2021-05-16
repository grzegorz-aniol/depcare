package org.appga.depcare.service.dependency

import mu.KLogging
import okhttp3.OkHttpClient
import okhttp3.Request
import org.eclipse.microprofile.reactive.messaging.Channel
import org.eclipse.microprofile.reactive.messaging.Emitter
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.eclipse.microprofile.reactive.messaging.OnOverflow
import org.neo4j.driver.Driver
import org.neo4j.driver.Query
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import java.util.concurrent.atomic.AtomicLong
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject
import javax.xml.parsers.DocumentBuilderFactory

@ApplicationScoped
class DependencyAnalyser(
	@Channel("out-deps")
	@OnOverflow(value = OnOverflow.Strategy.BUFFER, bufferSize = 10)
	private val payloadEmitter: Emitter<String>,
	@Inject
	private val driver: Driver
) {
	private companion object : KLogging()
	private val dependencyCounter = AtomicLong(0L)

	private val httpClient = OkHttpClient()

	fun postPomLink(url: String) {
		payloadEmitter.send(url)
	}

	@Incoming("in-deps")
	protected fun consumer(url: String) {
		val doc = fetchPomDocument(url)
		val rootElement = doc.documentElement
		if (rootElement == null) {
			logger.error { "Missing root xml element" }
			return
		}
		rootElement.run {
			val parentVersionIndication = getFirstElement("parent")?.let { getVersionIndication(it) }
			val thisVersionIndication = getVersionIndication(this)
			val name = getFirstElementValue("name")
			val actualVersionIndication = mergeVersionIndications(parentVersionIndication, thisVersionIndication)
			if (!actualVersionIndication.isValid()) {
				logger.error { "Cannot find library group, artifact or version in POM xml" }
				return
			}
			getFirstElement("dependencies")?.run {
				getElementsByTagName("dependency")?.forEach { dep ->
					val depIndication = getDependencyIndication(dep)
					if (!depIndication.isValid()) {
						logger.error { "Cannot find library dependency group, artifact or version" }
					} else {
						insertDependency(actualVersionIndication, depIndication)
					}
				}
			}
		}
	}

	private fun insertDependency(
		actualVersionIndication: VersionIndication,
		depIndication: VersionIndication
	) {
		val counter = dependencyCounter.incrementAndGet()
		logger.info { "Adding new dependency (total count: $counter)" }
		driver.session().use { session ->
			session.run(
				Query(
					"""
						MERGE (lv:Version {artifactId: ${'$'}artifactId, groupId: ${'$'}groupId, version: ${'$'}version}) 
						MERGE (dv: Version {artifactId: ${'$'}depArtifactId, groupId: ${'$'}depGroupId, version: ${'$'}depVersion})
						MERGE (lv)-[d:DEPENDS_ON]->(dv)
							ON CREATE SET d.scope=${'$'}scope, d.isOptional=${'$'}isOptional, d.type=${'$'}type
					""".trimIndent()
				).withParameters(
					mapOf(
						"groupId" to actualVersionIndication.group,
						"artifactId" to actualVersionIndication.artifact,
						"version" to actualVersionIndication.version,
						"depGroupId" to depIndication.group,
						"depArtifactId" to depIndication.artifact,
						"depVersion" to depIndication.version,
						"scope" to depIndication.scope,
						"isOptional" to depIndication.optional,
						"type" to depIndication.type,
					)
				)
			)
		}
	}

	private class VersionIndication(
		val group: String?,
		val artifact: String?,
		val version: String?,
		val scope: String? = null,
		val optional: Boolean? = false,
		val type: String? = null
	) {
		fun isValid(): Boolean =
			(group?.isNotBlank() ?: false && artifact?.isNotBlank() ?: false && version?.isNotBlank() ?: false)

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
			group = current?.group ?: parent?.group,
			artifact = current?.artifact,
			version = current?.version ?: parent?.version
		)
	}

	private fun Element.getFirstElementValue(name: String): String? {
		val element = firstChildElement(name)
		return element?.textContent
	}

	private fun Element.getFirstElement(name: String): Element? {
		return firstChildElement(name)
	}

	private fun NodeList.forEach(action: (Element) -> Unit) {
		for (i in 0..length) {
			val node = item(i)
			if (node is Element) {
				action(node)
			}
		}
	}

	private fun Element.firstChildElement(name: String): Element? {
		for (i in 0..childNodes.length) {
			val node = childNodes.item(i)
			if (node is Element) {
				if (node.tagName == name) {
					return node
				}
			}
		}
		return null
	}

	private fun fetchPomDocument(url: String): Document {
		val request = Request.Builder()
			.url(url)
			.addHeader("Connection", "keep-alive")
			.get()
			.build()

		val factory = DocumentBuilderFactory.newInstance()
		val builder = factory.newDocumentBuilder()

		return httpClient.newCall(request).execute().use { response ->
			logger.debug { "Response: ${response.code}" }
			if (response.code > 400) {
				throw IllegalStateException("Failed request: $url")
			}
			builder.parse(response.body?.byteStream())
		}
	}
}