package com.appga.depcare.domain

import com.appga.depcare.serialization.InstantSerializer
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class JvmLibraryVersion(
	val fileName: String?,
	val library: JvmLibrary,
	val version: String,
	val url: String,
	val pomUrl: String? = null,
	val jarUrl: String? = null,
	@Serializable(with = InstantSerializer::class)
	val publishedAt: Instant? = null,
	val approxFileSize: Long? = null
)

data class VersionIndication(
	val groupId: String?,
	val artifactId: String?,
	val version: String?,
	val scope: String? = null,
	val optional: Boolean? = false,
	val type: String? = null
) {
	fun isValid(): Boolean =
		(groupId?.isNotBlank() ?: false && artifactId?.isNotBlank() ?: false)

	fun hasVersion(): Boolean =
		version?.isNotBlank() ?: false
}