package org.appga.depcare.domain

import java.net.URL
import kotlinx.serialization.Serializable

@Serializable
data class MavenRepo(val name: String, val rootPath: String)

@Serializable
data class JvmLibrary(val name: String, val groupId: String, val artifactId: String, val url: String)

@Serializable
data class JvmLibraryVersion(
	val library: JvmLibrary,
	val version: String,
	val url: String
)

operator fun URL.plus(path: String): URL {
	return URL(this.toString().trim('/') + "/" + path.trim('/'))
}

@Serializable
sealed class MvnRepoDir {
	abstract val url: String
}

@Serializable
data class MvnRootDir(override val url: String) : MvnRepoDir()
@Serializable
data class MvnGroupDir(override val url: String, val groupId: String) : MvnRepoDir()
@Serializable
data class MvnLibraryDir(override val url: String, val groupId: String, val artifactId: String) : MvnRepoDir()
@Serializable
data class MvnVersionDir(override val url: String, val groupId: String, val artifactId: String, val version: String) :
	MvnRepoDir()

