package com.appga.depcare.supplier.kafka

import com.appga.depcare.domain.JvmLibrary
import com.appga.depcare.supplier.configuration.KafkaTopics
import com.appga.depcare.supplier.configuration.SerializerConfiguration
import kotlinx.serialization.SerializationStrategy
import mu.KLogging
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class LibraryMsgProducer(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val serializerConfig: SerializerConfiguration
) {
    private companion object : KLogging()

    private val serializer: SerializationStrategy<JvmLibrary> = JvmLibrary.serializer()

    fun postMessage(obj: JvmLibrary) {
        logger.debug { "Posting new message" }
        val payload = serializerConfig.jsonSerializer().encodeToString(serializer, obj)
        kafkaTemplate.send(KafkaTopics.TOPIC_LIBS, payload)
    }
}
