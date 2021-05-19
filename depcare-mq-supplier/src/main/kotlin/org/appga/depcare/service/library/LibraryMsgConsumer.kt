package org.appga.depcare.service.library

import kotlinx.serialization.DeserializationStrategy
import mu.KLogging
import org.appga.depcare.clients.MvnRepoClient
import org.appga.depcare.config.SerializerConfig
import org.appga.depcare.db.Repository
import org.appga.depcare.domain.JvmLibrary
import org.appga.depcare.domain.LibraryMetadata
import org.appga.depcare.utils.asSequence
import org.appga.depcare.utils.getFirstElement
import org.appga.depcare.utils.getFirstElementValue
import org.eclipse.microprofile.reactive.messaging.Incoming
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject

@ApplicationScoped
class LibraryMsgConsumer(
    @Inject
    private val config: SerializerConfig,
    @Inject
    private val mvnRepoClient: MvnRepoClient,
    @Inject
    private val repository: Repository
) {
    private companion object : KLogging()

    private val deserializer: DeserializationStrategy<JvmLibrary> = JvmLibrary.serializer()

    @Incoming("in-libs")
    fun process(payload: String) {
        logger.debug { "Consumer: library message" }
        try {
            val jvmLibrary = deserializePayload(payload)
            val metadata = fetchMetaFile(jvmLibrary)
            repository.saveLibrary(jvmLibrary, metadata)
        } catch (ex: Exception) {
            logger.error { ex.message }
        }
    }

    private fun deserializePayload(payload: String): JvmLibrary {
        return config.json.decodeFromString(deserializer, payload)
    }

    private fun fetchMetaFile(jvmLibrary: JvmLibrary): LibraryMetadata {
        logger.debug { "Fetching meta file ${jvmLibrary.metadataUrl}" }
        val document = mvnRepoClient.fetchXmlDocument(jvmLibrary.metadataUrl)
        var lastUpdatedText: String? = null
        var latest: String? = null
        var release: String? = null
        var versionsCount: Int? = null
        var lastUpdated: LocalDateTime? = null
        document.documentElement.run {
            getFirstElement("versioning")?.let {
                latest = getFirstElementValue("latest")
                release = getFirstElementValue("release")
                lastUpdatedText = getFirstElementValue("lastUpdated")
                getFirstElement("versions")?.let {
                    versionsCount = getElementsByTagName("version").asSequence().count()
                }
            }
        }
        if (lastUpdatedText != null) {
            val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
            lastUpdated = LocalDateTime.parse(lastUpdatedText, formatter)
        }
        return LibraryMetadata(
            latest = latest,
            release = release,
            lastUpdated = lastUpdated,
            versionsCount = versionsCount
        )
    }
}
