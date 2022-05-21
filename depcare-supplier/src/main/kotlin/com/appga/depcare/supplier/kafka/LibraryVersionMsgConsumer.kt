package com.appga.depcare.supplier.kafka

import com.appga.depcare.domain.JvmLibraryVersion
import com.appga.depcare.supplier.configuration.KafkaTopics
import com.appga.depcare.supplier.configuration.SerializerConfiguration
import com.appga.depcare.supplier.service.DependencyAnalyser
import kotlinx.serialization.DeserializationStrategy
import mu.KLogging
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component

@Component
class LibraryVersionMsgConsumer(
    private val config: SerializerConfiguration,
	private val dependencyAnalyser: DependencyAnalyser,

) {
    private val deserializer: DeserializationStrategy<JvmLibraryVersion> = JvmLibraryVersion.serializer()

    private companion object : KLogging()

	@KafkaListener(topics = [KafkaTopics.TOPIC_VERSIONS])
    fun process(@Payload payload: String) {
        logger.info { "Consumer: library version message" }
        val jvmLibraryVersion = deserializePayload(payload)
        try {
            dependencyAnalyser.saveVersionWithDependencies(libraryVersion = jvmLibraryVersion)
        } catch (ex: Exception) {
            logger.error("Error processing version: $jvmLibraryVersion", ex)
        }
    }

    private fun deserializePayload(payload: String): JvmLibraryVersion {
        return config.jsonSerializer().decodeFromString(deserializer, payload)
    }
}
