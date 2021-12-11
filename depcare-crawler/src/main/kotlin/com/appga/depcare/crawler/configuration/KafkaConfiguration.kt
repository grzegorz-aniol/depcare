package com.appga.depcare.crawler.configuration

import org.springframework.context.annotation.Configuration

@Configuration
class KafkaConfiguration {

	enum class KafkaQueues(val queueName: String) {
		LIBS("libs"),
		VERSIONS("versions")
	}
}