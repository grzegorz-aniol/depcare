package com.appga.depcare.domain

import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class JvmLibrary(
	val name: String,
	val groupId: String,
	val artifactId: String,
	val url: String,
	val metadataUrl: String
)

data class LibraryMetadata(
	val latest: String? = null,
	val release: String? = null,
	val lastUpdated: LocalDateTime? = null,
	val versionsCount: Int? = null,
)