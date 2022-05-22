package com.appga.depcare.crawler.crawling

import edu.uci.ics.crawler4j.crawler.CrawlController
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy


@Service
class CrawlerService(
	val crawlController: CrawlController,
	val crawlerFactory: MavenRepoCrawlerFactory,
) {
	private val logger = KotlinLogging.logger { }
	private var isLaunched = false

	@Value("\${depcare.crawler.numOfCrawlers}")
	var numOfCrawlers: Int = 4

	fun addSeed(url: String) {
		crawlController.addSeed(url)
		startCrawling()
	}

	fun startCrawling() {
		if (!isLaunched || crawlController.isFinished) {
			isLaunched = true
			logger.info("Starting crawler")
			crawlController.startNonBlocking(crawlerFactory, numOfCrawlers);
		}
	}

	fun stopCrawling() {
		logger.info("Stopping crawler controller")
		crawlController.shutdown()
		isLaunched = false
	}

	@PostConstruct
	fun onStart() {
		startCrawling()
	}

	@PreDestroy
	fun onStop() {
		stopCrawling()
	}

}