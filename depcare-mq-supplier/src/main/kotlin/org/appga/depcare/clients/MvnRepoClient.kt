package org.appga.depcare.clients

import mu.KLogging
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.atomic.AtomicLong
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class MvnRepoClient {

	private companion object : KLogging()
	private val httpClient = OkHttpClient()
	private val totalRequests = AtomicLong()

	fun fetchPage(url: String): String? {
		val request = Request.Builder()
			.url(url)
			.addHeader("Connection", "keep-alive")
			.get()
			.build()

		logger.debug { "Request: $url" }

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
}