package com.appga.depcare.crawler

import java.time.Instant

class FileMetadata(
    val name: String,
    val publishedAt: Instant? = null,
    val size: Long? = null
)

class PageContent(
    val url: String,
    val header: String,
    val links: List<String> = emptyList(),
    val files: Map<String, FileMetadata> = emptyMap(),
) {
    fun findJarLink(): String? {
        return links.firstOrNull { it.endsWith(".jar") && !it.contains("javadoc") && !it.contains("sources") }
    }

    fun findPomLink(): String? {
        return links.firstOrNull { it.endsWith(".pom") }
    }
}
