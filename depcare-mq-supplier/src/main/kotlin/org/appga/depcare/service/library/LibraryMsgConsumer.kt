package org.appga.depcare.service.library

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.serialization.DeserializationStrategy
import mu.KLogging
import org.appga.depcare.clients.MvnRepoClient
import org.appga.depcare.config.SerializerConfig
import org.appga.depcare.domain.JvmLibrary
import org.appga.depcare.utils.asSequence
import org.appga.depcare.utils.getFirstElement
import org.appga.depcare.utils.getFirstElementValue
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.neo4j.driver.Driver
import org.neo4j.driver.Query
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject

@ApplicationScoped
class LibraryMsgConsumer(
    @Inject
    private val config: SerializerConfig,
    @Inject
    private val mvnRepoClient: MvnRepoClient,
    @Inject
    private val driver: Driver
) {
    private companion object : KLogging()

    private val deserializer: DeserializationStrategy<JvmLibrary> = JvmLibrary.serializer()

    @Incoming("in-libs")
    fun process(payload: String) {
        logger.debug { "Consumer: library message" }
        try {
            val jvmLibrary = deserializePayload(payload)
            val metadata = fetchMetaFile(jvmLibrary)
            saveLibrary(jvmLibrary, metadata)
        } catch (ex: Exception) {
            logger.error { ex.message }
        }
    }

    private fun saveLibrary(jvmLibrary: JvmLibrary, metadata: LibraryMetadata) {
        driver.session().use { session ->
            logger.info { "Adding library" }
            session.run(
                Query(
                    """
                    MERGE (l:Library {groupId: ${'$'}groupId, artifactId: ${'$'}artifactId})
                        ON CREATE SET 
                            l.name=${'$'}name, l.url=${'$'}url, 
                            l.latestVersion=${'$'}latestVersion, l.releaseVersion=${'$'}releaseVersion, l.lastUpdated=${'$'}lastUpdated, 
                            l.versionsCount=${'$'}versionsCount
                        ON MATCH SET 
                            l.latestVersion=${'$'}latestVersion, l.releaseVersion=${'$'}releaseVersion, l.lastUpdated=${'$'}lastUpdated, 
                            l.versionsCount=${'$'}versionsCount
                    MERGE (g:Group {groupId: ${'$'}groupId})
                    MERGE (l)-[:MEMBER_OF]->(g)
                    """.trimIndent()
                ).withParameters(
                    mapOf(
                        "artifactId" to jvmLibrary.artifactId,
                        "groupId" to jvmLibrary.groupId,
                        "name" to jvmLibrary.name,
                        "url" to jvmLibrary.url,
                        "latestVersion" to metadata.latest,
                        "releaseVersion" to metadata.release,
                        "lastUpdated" to metadata.lastUpdated,
                        "versionsCount" to metadata.versionsCount
                    )
                )
            )
        }
    }

    private fun deserializePayload(payload: String): JvmLibrary {
        return config.json.decodeFromString(deserializer, payload)
    }

    private fun fetchMetaFile(jvmLibrary: JvmLibrary): LibraryMetadata {
        logger.debug { "Fetching meta file ${jvmLibrary.metadataUrl}" }
        val document = mvnRepoClient.fetchXmlDocument(jvmLibrary.metadataUrl)
        val metadata = LibraryMetadata()
        var lastUpdatedText: String? = null
        document.documentElement.run {
            getFirstElement("versioning")?.let {
                metadata.latest = getFirstElementValue("latest")
                metadata.release = getFirstElementValue("release")
                lastUpdatedText = getFirstElementValue("lastUpdated")
                getFirstElement("versions")?.let {
                    metadata.versionsCount = getElementsByTagName("version").asSequence().count()
                }
            }
        }
        if (lastUpdatedText != null) {
            val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
            metadata.lastUpdated = LocalDateTime.parse(lastUpdatedText, formatter)
        }
        return metadata
    }

    private class LibraryMetadata {
        var latest: String? = null
        var release: String? = null
        var lastUpdated: LocalDateTime? = null
        var versionsCount: Int? = null
    }
}
