package com.appga.depcare.crawler

import edu.uci.ics.crawler4j.crawler.CrawlController
import org.springframework.beans.factory.FactoryBean
import org.springframework.stereotype.Component

@Component
class MavenRepoCrawlerFactory(
	private val pageAnalyzer: PageAnalyzer,
	private val libraryQueueProducer: LibraryQueueProducer,
	private val libraryVersionQueueProducer: LibraryVersionQueueProducer,
) : CrawlController.WebCrawlerFactory<MavenRepoCrawler>, FactoryBean<MavenRepoCrawler> {

	override fun newInstance(): MavenRepoCrawler {
		return MavenRepoCrawler(pageAnalyzer, libraryQueueProducer, libraryVersionQueueProducer)
	}

	override fun getObject(): MavenRepoCrawler? {
		return newInstance()
	}

	override fun getObjectType(): Class<*>? {
		return MavenRepoCrawler::class.java
	}
}