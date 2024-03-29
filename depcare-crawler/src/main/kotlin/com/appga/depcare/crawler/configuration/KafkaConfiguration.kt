package com.appga.depcare.crawler.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.TopicBuilder

private val NUM_PARTITIONS = 16

@Configuration
class KafkaConfiguration {

	enum class KafkaQueues(val queueName: String) {
		LIBS("libs"),
		VERSIONS("versions")
	}

	@Bean
	fun libsTopic() =
		TopicBuilder.name(KafkaQueues.LIBS.queueName)
			.partitions(NUM_PARTITIONS)
			.replicas(1)
			.build()

	@Bean
	fun versionsTopic() =
		TopicBuilder.name(KafkaQueues.VERSIONS.queueName)
			.partitions(NUM_PARTITIONS)
			.replicas(1)
			.build()

}