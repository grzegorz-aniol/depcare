package com.appga.depcare.config

import io.vertx.amqp.AmqpClientOptions
import javax.enterprise.context.ApplicationScoped
import javax.enterprise.inject.Produces
import javax.inject.Named

@ApplicationScoped
class AmqpConfig {

	@Produces
	@Named("amqp-options")
	fun getNamedOptions(): AmqpClientOptions? {
		return AmqpClientOptions()
			.setHost("localhost")
			.setPort(61616)
			.setConnectTimeout(1000)
			.setReconnectInterval(10000)
	}
}