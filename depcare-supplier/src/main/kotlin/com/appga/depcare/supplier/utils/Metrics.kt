package com.appga.depcare.supplier.utils

import mu.KLogging
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicLong

@Component
class Metrics {

	private companion object : KLogging()
    private val mvnRepoPages = AtomicLong()
	private val mvnDocumentsFetched = AtomicLong()
    private val libraryAdded = AtomicLong()
	private val versionAdded = AtomicLong()

	fun mvnRepoPageVisited(url: String) {
		val cnt = mvnRepoPages.incrementAndGet()
		logger.debug { "Page visited $url (total: $cnt)"}
	}

	fun mvnRepoDocumentFetched(url: String) {
		val cnt = mvnDocumentsFetched.incrementAndGet()
		logger.debug { "Document fetched $url (total: $cnt)" }
	}

	fun dbLibraryAdded(name: String) {
		val cnt = libraryAdded.incrementAndGet()
		logger.debug { "Library added $name (total: $cnt)"}
	}

	fun dbVersionAdded(name: String) {
		val cnt = versionAdded.incrementAndGet()
		logger.debug { "Version added $name (total: $cnt)"}
	}
}