package com.appga.depcare.supplier.metrics

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct


@Component
class MetricsService(private val meterRegistry: MeterRegistry) {

//	lateinit var repositorySaveLibrary: Timer
//	lateinit var repositorySaveVersion: Timer

	@PostConstruct
	fun setupMetrics() {
//		repositorySaveLibrary = Timer.builder("repository.save-library")
//			.description("Saving or merging library object")
//			.register(meterRegistry)
//
//		repositorySaveVersion = Timer.builder("repository.save-version")
//			.description("Saving or merging version object")
//			.register(meterRegistry)
	}
}