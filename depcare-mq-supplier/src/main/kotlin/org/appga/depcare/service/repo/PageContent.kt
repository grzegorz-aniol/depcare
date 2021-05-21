package org.appga.depcare.service.repo

import java.time.LocalDateTime

class PageContent(
    val url: String,
    val header: String,
    val links: List<String> = emptyList(),
    val files: Map<String, LocalDateTime> = emptyMap(),
)
