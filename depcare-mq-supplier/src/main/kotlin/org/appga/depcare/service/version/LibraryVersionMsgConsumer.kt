package org.appga.depcare.service.version

import kotlinx.serialization.DeserializationStrategy
import mu.KLogging
import org.appga.depcare.config.SerializerConfig
import org.appga.depcare.db.Repository
import org.appga.depcare.domain.JvmLibraryVersion
import org.eclipse.microprofile.reactive.messaging.Incoming
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject

@ApplicationScoped
class LibraryVersionMsgConsumer(
    @Inject
    private val config: SerializerConfig,
    @Inject
    private val repository: Repository
) {
    private val deserializer: DeserializationStrategy<JvmLibraryVersion> = JvmLibraryVersion.serializer()

    private companion object : KLogging()

    @Incoming("in-versions")
    fun process(payload: String) {
        logger.debug { "Consumer: library version message" }
        val jvmLibraryVersion = deserializePayload(payload)
        try {
            repository.saveLibraryVersion(jvmLibraryVersion)
        } catch (ex: Exception) {
            logger.error { ex.message }
        }
    }

    private fun deserializePayload(payload: String): JvmLibraryVersion {
        return config.json.decodeFromString(deserializer, payload)
    }
}
