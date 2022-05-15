package com.appga.depcare.crawler

import edu.uci.ics.crawler4j.crawler.CrawlController
import mu.KotlinLogging
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

@RestController
@RequestMapping
class MavenCrawlController(
	val crawlController: CrawlController,
	val crawlerFactory: MavenRepoCrawlerFactory,
) {
	private val logger = KotlinLogging.logger { }

	@PostConstruct
	fun onStart() {
		logger.info("Starting crawler")
		crawlController.startNonBlocking(crawlerFactory, 1);
	}

	@PreDestroy
	fun onStop() {
		logger.info("Stopping crawler controller")
		crawlController.shutdown()
	}

	@PostMapping(path = ["/api/repo/url"], consumes = [MediaType.TEXT_PLAIN_VALUE])
	fun postPath(@RequestBody path: String) {
		logger.debug { "Pushing new url for crawler: $path" }
		crawlController.addSeed(path)
		if (crawlController.isFinished) {
			logger.info("Starting crawler")
			crawlController.startNonBlocking(crawlerFactory, 1);
		}
	}
}