package com.appga.depcare.crawler.configuration

import edu.uci.ics.crawler4j.crawler.CrawlConfig
import edu.uci.ics.crawler4j.crawler.CrawlController
import edu.uci.ics.crawler4j.fetcher.PageFetcher
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer
import org.apache.http.message.BasicHeader
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CrawlerConfiguration {

	@Bean
	fun crawlConfig(): CrawlConfig {
		val config = CrawlConfig()
		config.crawlStorageFolder = ".crawler"
		config.isShutdownOnEmptyQueue = false
		config.isFollowRedirects = true
		config.connectionTimeout = 10000
		config.socketTimeout = 30000
		config.isIncludeHttpsPages = true
		config.isProcessBinaryContentInCrawling = false
		config.isProcessBinaryContentInCrawling = false
		config.politenessDelay = 30
		config.isResumableCrawling = true
		config.isRespectNoIndex = false
		config.isRespectNoFollow = false
		config.defaultHeaders.addAll(listOf(
			BasicHeader("Accept","application/xml, */*")
		))
		config.userAgentString = "Maven Dependency Crawler"
		return config
	}

	@Bean
	fun pageFetcher(crawlConfig: CrawlConfig): PageFetcher = PageFetcher(crawlConfig)

	@Bean
	fun robotstxtConfig() = RobotstxtConfig()

	@Bean
	fun robotstxtServer(robotstxtConfig: RobotstxtConfig, pageFetcher: PageFetcher) = RobotstxtServer(robotstxtConfig, pageFetcher)

	@Bean
	fun crawlerController(crawlConfig: CrawlConfig, pageFetcher: PageFetcher, robotstxtServer: RobotstxtServer) =
		CrawlController(crawlConfig, pageFetcher, robotstxtServer)

}