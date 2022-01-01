package com.appga.depcare.crawler.configuration

import io.micrometer.core.aop.TimedAspect
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.EnableAspectJAutoProxy

@Configuration
@EnableAspectJAutoProxy
class AspectConfiguration {

	@Bean
	fun timedAspect(registry: MeterRegistry): TimedAspect {
		return TimedAspect(registry)
	}

}