package com.appga.depcare.crawler.analyzers

import com.appga.depcare.crawler.FileMetadata
import com.appga.depcare.crawler.PageContent
import mu.KLogging
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.roundToLong

@Component
class PageAnalyzer {
	private companion object : KLogging()

	private val regexLink = Regex("<a href=\"([^\"]+)\"")
	private val regexHeader = Regex("<h1>(?:Index of )?(.+)</h1>")
	private val regexFiles = Regex("<a href=\"([^\"]+)\".+<\\/a>\\s+(\\S{2,4}-\\S{2,4}-\\S{2,4} \\d{2}:\\d{2})\\s+([\\.\\d]+)\\s([kKmMgG]B)?")
	private val datFormats = listOf(
		DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
		DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm"),
	)

	fun analyse(body: String, url: String): PageContent {
		val links = regexLink.findAll(body).mapNotNull {
			val link = it.groups[1]?.value
			if (link =="../" || link == "..") {
				return@mapNotNull null
			}
			link
		}.toList()
		if (links.isEmpty()) {
			logger.debug { "Links not found for page: $url" }
		}

		val files = regexFiles.findAll(body).mapNotNull {
			val fileName = it.groups[1]?.value!!
			val dateWithTime = it.groups[2]?.value
			val dateTime = dateWithTime?.let { dt -> parseDateTime(dt) }
				?: throw IllegalStateException("Cannot parse date/time: '$dateWithTime'. Page: $url")
			val sizeValue = it.groups[3]?.value?.toDouble()
			val sizeScale = it.groups[4]?.value?.toUpperCase()
			if (sizeValue == null) {
				return@mapNotNull null
			}
			val size = when (sizeScale) {
				"KB" -> 1024 * sizeValue
				"MB" -> 1024 * 1024 * sizeValue
				"GB" -> 1024 * 1024 * 1024 * sizeValue
				else -> sizeValue
			}
			fileName to FileMetadata(
				name = fileName,
				publishedAt = dateTime,
				size = size.roundToLong()
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

	private fun parseDateTime(input: String): Instant? {
		return datFormats.mapNotNull { pattern ->
			try {
				LocalDateTime.parse(input, pattern).toInstant(ZoneOffset.UTC)
			} catch (ex: Exception) {
				null
			}
		}.firstOrNull()
	}
}