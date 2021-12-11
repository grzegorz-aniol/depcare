package com.appga.depcare.supplier.clients

import com.appga.depcare.utils.Metrics
import mu.KLogging
import okhttp3.OkHttpClient
import okhttp3.Request
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.w3c.dom.Document
import javax.xml.parsers.DocumentBuilderFactory

@Component
@Scope("prototype")
class MvnRepoClient(
    private val metrics: Metrics,
) {
    private companion object : KLogging()

    private val httpClient = OkHttpClient()

    fun fetchPage(url: String): String? {
        logger.debug { "Request: $url" }

        val request = Request.Builder()
            .url(url)
            .addHeader("Connection", "keep-alive")
            .get()
            .build()

        val body = httpClient.newCall(request).execute().use { response ->
            metrics.mvnRepoPageVisited(url)
            logger.debug { "Response: ${response.code}" }
            if (response.code > 400) {
                throw IllegalStateException("Failed request: $url")
            }
            response.body?.string()
        }

        if (body.isNullOrEmpty()) {
            logger.warn { "Empty response for $url" }
            return null
        }

        return body
    }

    fun fetchXmlDocument(url: String): Document {
        logger.debug { "Fetching XML document from $url" }

        val request = Request.Builder()
            .url(url)
            .addHeader("Connection", "keep-alive")
            .get()
            .build()

        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()

        return httpClient.newCall(request).execute().use { response ->
            metrics.mvnRepoDocumentFetched(url)
            logger.debug { "Response: ${response.code}" }
            if (response.code >= 400) {
                throw IllegalStateException("Failed request: $url")
            }
            builder.parse(response.body?.byteStream())
        }
    }
}
