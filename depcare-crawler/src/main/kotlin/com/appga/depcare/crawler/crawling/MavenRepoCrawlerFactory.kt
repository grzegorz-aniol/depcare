package com.appga.depcare.crawler.crawling

import edu.uci.ics.crawler4j.crawler.CrawlController
import mu.KotlinLogging
import org.springframework.beans.factory.BeanFactory
import org.springframework.stereotype.Component

@Component
class MavenRepoCrawlerFactory(
	private val beanFactory: BeanFactory,
) : CrawlController.WebCrawlerFactory<MavenRepoCrawler> {

	private val logger = KotlinLogging.logger {}

	override fun newInstance(): MavenRepoCrawler {
		logger.debug { "Requesting new crawler instance" }
		return beanFactory.getBean("mavenRepoCrawler", MavenRepoCrawler::class.java)
	}

}