package com.appga.depcare.supplier.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.TopicBuilder

private val NUM_PARTITIONS = 16

@Configuration
class KafkaConfiguration {

	@Bean
	fun libsTopic() =
		TopicBuilder.name(KafkaTopics.TOPIC_LIBS)
			.partitions(NUM_PARTITIONS)
			.replicas(1)
			.build()

	@Bean
	fun versionsTopic() =
		TopicBuilder.name(KafkaTopics.TOPIC_VERSIONS)
			.partitions(NUM_PARTITIONS)
			.replicas(1)
			.build()

	@Bean
	fun depsTopic() =
		TopicBuilder.name(KafkaTopics.TOPIC_DEPS)
			.partitions(NUM_PARTITIONS)
			.replicas(1)
			.build()

}