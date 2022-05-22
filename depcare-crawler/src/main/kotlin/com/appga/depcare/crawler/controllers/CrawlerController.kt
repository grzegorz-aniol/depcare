package com.appga.depcare.crawler.controllers

import com.appga.depcare.crawler.crawling.CrawlerService
import mu.KotlinLogging
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/crawler")
class CrawlerController(
	private val crawlerService: CrawlerService
) {
	private val logger = KotlinLogging.logger { }

	@PostMapping(path = ["/url"], consumes = [MediaType.TEXT_PLAIN_VALUE])
	fun postPath(@RequestBody url: String) {
		logger.debug { "Pushing new url for crawler: $url" }
		crawlerService.addSeed(url)
	}
}