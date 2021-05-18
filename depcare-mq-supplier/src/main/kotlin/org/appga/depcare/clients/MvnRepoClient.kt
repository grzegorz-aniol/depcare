package org.appga.depcare.clients

import mu.KLogging
import okhttp3.OkHttpClient
import okhttp3.Request
import org.w3c.dom.Document
import java.util.concurrent.atomic.AtomicLong
import javax.enterprise.context.ApplicationScoped
import javax.xml.parsers.DocumentBuilderFactory

@ApplicationScoped
class MvnRepoClient {

    private companion object : KLogging()

    private val httpClient = OkHttpClient()
    private val totalRequests = AtomicLong()

    fun fetchPage(url: String): String? {
        logger.debug { "Request: $url" }

        val request = Request.Builder()
            .url(url)
            .addHeader("Connection", "keep-alive")
            .get()
            .build()

        val body = httpClient.newCall(request).execute().use { response ->
            logger.debug { "Response: ${response.code}" }
            if (response.code > 400) {
                throw IllegalStateException("Failed request: $url")
            }
            totalRequests.incrementAndGet()
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
            logger.debug { "Response: ${response.code}" }
            if (response.code > 400) {
                throw IllegalStateException("Failed request: $url")
            }
            builder.parse(response.body?.byteStream())
        }
    }
}
