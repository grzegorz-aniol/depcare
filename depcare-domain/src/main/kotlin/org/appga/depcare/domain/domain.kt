package org.appga.depcare.domain

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.lang.Integer.max
import java.net.URL
import java.time.LocalDateTime
import java.time.ZoneOffset

open class LocalDateTimeSerializer : KSerializer<LocalDateTime> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("java.time.LocalDateTime", PrimitiveKind.LONG)
    override fun deserialize(decoder: Decoder): LocalDateTime {
        return LocalDateTime.ofEpochSecond(decoder.decodeLong(), 0, ZoneOffset.UTC)
    }

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        encoder.encodeLong(value.toEpochSecond(ZoneOffset.UTC))
    }
}

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
    val url: String,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime? = null,
)

operator fun URL.plus(path: String): URL {
    return URL(this.toString().trim('/') + "/" + path.trim('/'))
}

@Serializable
sealed class MvnRepoDir {
    abstract val url: String
    /** Return depth of the repository. Simply count slashed minus 4 for 'https://repo1.maven.org/maven2/' */
    val level: Int get() = max(0, url.count { it == '/' } - 4)
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
data class MvnVersionDir(
    override val url: String,
    val groupId: String,
    val artifactId: String,
    val version: String,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime? = null,
) : MvnRepoDir()

data class LibraryMetadata(
    val latest: String? = null,
    val release: String? = null,
    val lastUpdated: LocalDateTime? = null,
    val versionsCount: Int? = null,
)

class VersionIndication(
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
