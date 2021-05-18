package org.appga.depcare.domain

import kotlinx.serialization.Serializable
import java.net.URL

@Serializable
data class MavenRepo(val name: String, val rootPath: String)

@Serializable
data class JvmLibrary(
    val name: String,
    val groupId: String,
    val artifactId: String,
    val url: String,
    val metadataUrl: String
)

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
data class MvnLibraryDir(
    override val url: String,
    val groupId: String,
    val artifactId: String,
    val metadataUrl: String,
) : MvnRepoDir()

@Serializable
data class MvnVersionDir(override val url: String, val groupId: String, val artifactId: String, val version: String) :
    MvnRepoDir()
