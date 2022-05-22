package com.appga.depcare.crawler.kafka

import com.appga.depcare.crawler.configuration.KafkaConfiguration
import com.appga.depcare.crawler.metrics.MetricsService
import com.appga.depcare.domain.JvmLibraryVersion
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class LibraryVersionQueueProducer(
	private val kafkaTemplate: KafkaTemplate<String, String>,
	private val jsonSerializable: Json,
	private val metricsService: MetricsService
) {
	private val logger = KotlinLogging.logger { }
	private val serializer: SerializationStrategy<JvmLibraryVersion> = JvmLibraryVersion.serializer()

	fun send(libraryVersion: JvmLibraryVersion) {
		logger.debug { "Sending payload with library version object" }
		val payload = jsonSerializable.encodeToString(serializer, libraryVersion)
		kafkaTemplate.send(KafkaConfiguration.KafkaQueues.VERSIONS.queueName, payload)
		metricsService.tickVersionsCounter()
	}
}