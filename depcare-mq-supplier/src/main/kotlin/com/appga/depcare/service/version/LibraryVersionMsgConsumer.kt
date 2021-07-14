package com.appga.depcare.service.version

import kotlinx.serialization.DeserializationStrategy
import mu.KLogging
import com.appga.depcare.config.SerializerConfig
import com.appga.depcare.db.Repository
import com.appga.depcare.domain.JvmLibraryVersion
import com.appga.depcare.service.dependency.DependencyAnalyser
import org.eclipse.microprofile.reactive.messaging.Incoming
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject

@ApplicationScoped
class LibraryVersionMsgConsumer(
    @Inject
    private val config: SerializerConfig,
    @Inject
    private val repository: Repository,
	@Inject
	private val dependencyAnalyser: DependencyAnalyser,

) {
    private val deserializer: DeserializationStrategy<JvmLibraryVersion> = JvmLibraryVersion.serializer()

    private companion object : KLogging()

    @Incoming("in-versions")
    fun process(payload: String) {
        logger.debug { "Consumer: library version message" }
        val jvmLibraryVersion = deserializePayload(payload)
        try {
            repository.saveLibraryVersion(jvmLibraryVersion)
            if (jvmLibraryVersion.pomUrl != null) {
                dependencyAnalyser.postPomLink(jvmLibraryVersion.pomUrl!!)
            } else {
                logger.warn { "Cannot find POM for version ${jvmLibraryVersion.url}"}
            }
        } catch (ex: Exception) {
            logger.error { ex.message }
        }
    }

    private fun deserializePayload(payload: String): JvmLibraryVersion {
        return config.json.decodeFromString(deserializer, payload)
    }
}
