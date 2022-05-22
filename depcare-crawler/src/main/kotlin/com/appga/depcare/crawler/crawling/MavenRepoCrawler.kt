package com.appga.depcare.crawler.crawling

import com.appga.depcare.crawler.analyzers.MavenAnalyzer
import edu.uci.ics.crawler4j.crawler.Page
import edu.uci.ics.crawler4j.crawler.WebCrawler
import edu.uci.ics.crawler4j.url.WebURL
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope("prototype")
open class MavenRepoCrawler(
	private val mavenAnalyzer: MavenAnalyzer
) : WebCrawler() {

	override fun shouldVisit(referringPage: Page?, url: WebURL?): Boolean {
		return mavenAnalyzer.shouldVisit(referringPage, url)
	}

	override fun shouldFollowLinksIn(url: WebURL?): Boolean {
		return super.shouldFollowLinksIn(url)
	}

	override fun visit(page: Page?) {
		mavenAnalyzer.visit(page)
	}

}