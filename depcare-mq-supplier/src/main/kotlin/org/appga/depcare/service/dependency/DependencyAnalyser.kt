package org.appga.depcare.service.dependency

import mu.KLogging
import org.appga.depcare.clients.MvnRepoClient
import org.appga.depcare.utils.forEach
import org.appga.depcare.utils.getFirstElement
import org.appga.depcare.utils.getFirstElementValue
import org.eclipse.microprofile.reactive.messaging.Channel
import org.eclipse.microprofile.reactive.messaging.Emitter
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.eclipse.microprofile.reactive.messaging.OnOverflow
import org.neo4j.driver.Driver
import org.neo4j.driver.Query
import org.w3c.dom.Element
import java.util.concurrent.atomic.AtomicLong
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
    private val driver: Driver
) {
    private companion object : KLogging()

    private val dependencyCounter = AtomicLong(0L)

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
                            saveDependency(actualVersionIndication, depIndication)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            logger.error { "Error: ${e.message}. Url: $url" }
        }
    }

    private fun saveDependency(
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
}
