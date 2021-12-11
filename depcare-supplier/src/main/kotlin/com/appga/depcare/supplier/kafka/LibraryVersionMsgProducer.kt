package com.appga.depcare.supplier.kafka

import com.appga.depcare.domain.JvmLibraryVersion
import com.appga.depcare.supplier.configuration.KafkaTopics
import com.appga.depcare.supplier.configuration.SerializerConfiguration
import kotlinx.serialization.SerializationStrategy
import mu.KLogging
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class LibraryVersionMsgProducer(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val serializerConfig: SerializerConfiguration
) {
    private companion object : KLogging()

    private val serializer: SerializationStrategy<JvmLibraryVersion> = JvmLibraryVersion.serializer()

    fun postMessage(obj: JvmLibraryVersion) {
        logger.debug { "Posting new message" }
        val payload = serializerConfig.jsonSerializer().encodeToString(serializer, obj)
        kafkaTemplate.send(KafkaTopics.TOPIC_VERSIONS, payload)
    }
}
