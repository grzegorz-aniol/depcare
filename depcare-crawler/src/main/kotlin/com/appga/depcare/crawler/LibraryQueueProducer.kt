package com.appga.depcare.crawler

import com.appga.depcare.crawler.configuration.KafkaConfiguration
import com.appga.depcare.crawler.metrics.MetricsService
import com.appga.depcare.domain.JvmLibrary
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class LibraryQueueProducer(
	private val kafkaTemplate: KafkaTemplate<String, String>,
	private val jsonSerializable: Json,
	private val metricsService: MetricsService
) {
	private val logger = KotlinLogging.logger { }
	private val serializer: SerializationStrategy<JvmLibrary> = JvmLibrary.serializer()

	fun send(library: JvmLibrary) {
		logger.debug { "Sending payload with library object" }
		val payload = jsonSerializable.encodeToString(serializer, library)
		kafkaTemplate.send(KafkaConfiguration.KafkaQueues.LIBS.queueName, payload)
		metricsService.tickLibrariesCounter()
	}
}