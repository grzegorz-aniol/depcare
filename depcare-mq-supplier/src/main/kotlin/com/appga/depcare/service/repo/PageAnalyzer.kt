package com.appga.depcare.service.repo

import io.quarkus.cache.CacheResult
import mu.KLogging
import com.appga.depcare.clients.MvnRepoClient
import java.time.LocalDateTime
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
    private val regexFiles = Regex("<a href=\"([^\"]+)\".+(\\d{4})-(\\d{2})-(\\d{2}) (\\d{2}):(\\d{2})\\s+(\\d+).+")

    @CacheResult(cacheName = "pages-cache")
    fun fetchPageContent(url: String): PageContent {
        val body = mvnRepoClient.fetchPage(url)

        require(!body.isNullOrEmpty()) { "Empty response" }
        val links = regexLink.findAll(body).mapNotNull { it.groups[1]?.value }.toList()
        if (links.isEmpty()) {
            logger.debug { "Links not found for page: $url" }
        }

        val files = regexFiles.findAll(body).mapNotNull {
            val fileName = it.groups[1]?.value!!
            val year = it.groups[2]?.value?.toInt()!!
            val month = it.groups[3]?.value?.toInt()!!
            val day = it.groups[4]?.value?.toInt()!!
            val hour = it.groups[5]?.value?.toInt()!!
            val minute = it.groups[6]?.value?.toInt()!!
            val fileSize = it.groups[7]?.value?.toLong()!!
            fileName to FileMetadata(
                name = fileName,
                createdAt = LocalDateTime.of(year, month, day, hour, minute),
                size = fileSize
            )
        }.toMap()

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
            files = files,
        )
    }
}
