package com.appga.depcare.domain

import kotlinx.serialization.Serializable
import java.net.URL
import java.time.LocalDateTime

operator fun URL.plus(path: String): URL {
	return URL(this.toString().trim('/') + "/" + path.trim('/'))
}

@Serializable
sealed class MvnRepoDir {
	abstract val url: String

	/** Return depth of the repository. Simply count slashed minus 4 for 'https://repo1.maven.org/maven2/' */
	val level: Int get() = Integer.max(0, url.count { it == '/' } - 4)
}

@Serializable
data class MvnUndetectedDir(override val url: String) : MvnRepoDir()

@Serializable
data class MvnRootDir(override val url: String) : MvnRepoDir()

@Serializable
data class MvnGroupDir(override val url: String, val groupId: String) : MvnRepoDir()

@Serializable
data class MvnLibraryDir(
	override val url: String,
	val groupId: String,
	val artifactId: String,
	val metadataUrl: String,
) : MvnRepoDir()

@Serializable
data class MvnVersionDir(
	override val url: String,
	val pomUrl: String? = null,
	val jarUrl: String? = null,
	val fileName: String,
	val groupId: String,
	val artifactId: String,
	val version: String,
	@Serializable(with = LocalDateTimeSerializer::class)
	val createdAt: LocalDateTime? = null,
	val fileSize: Long? = null,
) : MvnRepoDir()
