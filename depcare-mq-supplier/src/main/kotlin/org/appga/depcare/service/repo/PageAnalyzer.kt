package org.appga.depcare.service.repo

import io.quarkus.cache.CacheResult
import mu.KLogging
import org.appga.depcare.clients.MvnRepoClient
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject

@ApplicationScoped
class PageAnalyzer(
    @Inject
    val mvnRepoClient: MvnRepoClient
) {
    private companion object : KLogging()

    private val regexLink = Regex("<a href=\"([^\"]+)\"")
    private val regexHeader = Regex("<h1>(?:Index of /maven2/)?(.+)</h1>")

    @CacheResult(cacheName = "pages-cache")
    fun fetchPageContent(url: String): PageContent {
        val body = mvnRepoClient.fetchPage(url)

        require(!body.isNullOrEmpty()) { "Empty response" }
        val links = regexLink.findAll(body).mapNotNull { it.groups[1]?.value }.toList()
        if (links.isEmpty()) {
            logger.debug { "Links not found for page: $url" }
        }

        val header = regexHeader.find(body)?.groups?.get(1)?.value?.trim('.')
        if (header?.contains("Index of ") == true) {
            logger.warn { "Ignoring page $url" }
            return PageContent(url, "", emptyList())
        }

        if (header.isNullOrEmpty()) {
            logger.warn { "Header not found for page: $url" }
        }
        return PageContent(
            url = url,
            header = header ?: "",
            links = links,
        )
    }
}
