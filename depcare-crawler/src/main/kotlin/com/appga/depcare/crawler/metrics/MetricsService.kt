package com.appga.depcare.crawler.metrics

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct


@Component
class MetricsService(private val meterRegistry: MeterRegistry) {

	lateinit var pagesCounter: Counter
	lateinit var librariesCounter: Counter
	lateinit var versionsCounter: Counter
	lateinit var crawlerPageAnalyzeTime: Timer
	lateinit var crawlerUrlCheckTime: Timer

	@PostConstruct
	fun setupMetrics() {
		pagesCounter = Counter.builder("repository.pages.fetched")
			.description("Number of pages analyzed from maven repositories by web crawler")
			.register(meterRegistry)

		librariesCounter = Counter.builder("repository.libraries.count")
			.description("Number of libraries found during crawling")
			.register(meterRegistry)

		versionsCounter = Counter.builder("repository.versions.count")
			.description("Number of versions found during crawling")
			.register(meterRegistry)

		crawlerPageAnalyzeTime = Timer.builder("crawler.visit-time")
			.description("Analysing visited page")
			.register(meterRegistry)

		crawlerUrlCheckTime = Timer.builder("crawler.should-visit-time")
			.description("Checking if page should be visited")
			.register(meterRegistry)
	}

	fun tickPagesCounter() {
		pagesCounter.increment()
	}

	fun tickLibrariesCounter() {
		librariesCounter.increment()
	}

	fun tickVersionsCounter() {
		versionsCounter.increment()
	}
}
