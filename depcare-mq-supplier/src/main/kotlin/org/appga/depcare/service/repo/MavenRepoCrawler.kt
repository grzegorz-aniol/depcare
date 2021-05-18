package org.appga.depcare.service.repo

import mu.KLogging
import org.appga.depcare.clients.MvnRepoClient
import org.appga.depcare.domain.JvmLibrary
import org.appga.depcare.domain.JvmLibraryVersion
import org.appga.depcare.domain.MvnGroupDir
import org.appga.depcare.domain.MvnLibraryDir
import org.appga.depcare.domain.MvnRepoDir
import org.appga.depcare.domain.MvnRootDir
import org.appga.depcare.domain.MvnVersionDir
import org.appga.depcare.service.dependency.DependencyAnalyser
import org.appga.depcare.service.library.LibraryMsgProducer
import org.appga.depcare.service.version.LibraryVersionMsgProducer
import java.util.concurrent.atomic.AtomicLong
import javax.enterprise.context.Dependent
import javax.inject.Inject

private const val MAVEN_METADATA_FILE = "maven-metadata.xml"
private const val ARCHETYPE_CATALOG_NAME = "archetype-catalog.xml"
private const val ROBOTS_FILE_NAME = "robots.txt"

@Dependent
class MavenRepoCrawler(
    @Inject
    private val pageAnalyzer: PageAnalyzer,
    @Inject
    private val libraryProducer: LibraryMsgProducer,
    @Inject
    private val libraryVersionMsgProducer: LibraryVersionMsgProducer,
    @Inject
    private val dependencyAnalyser: DependencyAnalyser
) {
    private companion object : KLogging()

    private val regexVersionDir = Regex("\\d+\\.\\d+(\\.[\\w\\d]+)*/")
    private val totalLibraries = AtomicLong()
    private val totalVersions = AtomicLong()

    // root -> package -> package ... -> library dir -> version dir
    fun analyseRepoDirContent(url: String): List<MvnRepoDir> {
        logger.info { "Analysing repository directory $url" }

        val pageContent = pageAnalyzer.fetchPageContent(url)
        val repoDir = buildMvnRepoObject(pageContent)

        when (repoDir) {
            is MvnLibraryDir -> {
                val cnt = totalLibraries.incrementAndGet()
                logger.info { "Library: ${repoDir.artifactId}, total: $cnt" }
                val library = JvmLibrary(
                    name = repoDir.artifactId,
                    groupId = repoDir.groupId,
                    artifactId = repoDir.artifactId,
                    url = repoDir.url,
                    metadataUrl = repoDir.metadataUrl
                )
                libraryProducer.postMessage(library)
            }
            is MvnVersionDir -> {
                val cnt = totalVersions.incrementAndGet()
                logger.info { "Library version: ${repoDir.artifactId}, total: $cnt" }
            }
            is MvnGroupDir -> {
                logger.info { "Package: ${repoDir.groupId}" }
            }
            else -> Unit
        }

        if (pageContent.links.isEmpty()) {
            logger.warn { "Empty page content for url: ${repoDir.url}" }
            return emptyList()
        }

        logger.debug {
            "Page: ${pageContent.url}, header: ${pageContent.header}, links count: ${
            pageContent.links
                .size
            }"
        }

        val result = pageContent.links
            .filter { it != "../" }
            .mapNotNull { link ->
                val entryName = link.substringBeforeLast("/")
                val entryLink = repoDir.url + link
                when {
                    link.endsWith("/") -> {
                        buildMvnSubDirObj(repoDir, entryLink, entryName)
                    }
                    entryName != ".." -> {
                        when {
                            link.endsWith(".pom") -> {
                                logger.info { "Library POM: $entryName" }
                                dependencyAnalyser.postPomLink(entryLink)
                            }
                            link.endsWith(".jar") && !link.contains("javadoc") && !link.contains("sources") -> {
                                logger.info { "Library JAR: $entryName" }
                                when (repoDir) {
                                    is MvnVersionDir -> {
                                        val libraryVersion = JvmLibraryVersion(
                                            library = JvmLibrary(
                                                name = repoDir.artifactId,
                                                groupId = repoDir.groupId,
                                                artifactId = repoDir.artifactId,
                                                url = repoDir.url,
                                                metadataUrl = repoDir.url + MAVEN_METADATA_FILE
                                            ),
                                            version = repoDir.version,
                                            url = entryLink
                                        )
                                        libraryVersionMsgProducer.postMessage(libraryVersion)
                                    }
                                    else -> {
                                        logger.warn { "JAR found in non library folder ($entryLink)" }
                                    }
                                }
                            }
                            else -> MvnGroupDir(
                                url = entryLink,
                                groupId = when (repoDir) {
                                    is MvnRootDir -> entryName
                                    is MvnGroupDir -> "${repoDir.groupId}.$entryName"
                                    else -> ""
                                }
                            )
                        }
                        null
                    }
                    else -> {
                        logger.warn { "Undetected page link: $entryName" }
                        null
                    }
                }
            }

        logger.debug { "Page crawling result. Number of sub-folders: ${result.size}" }

        return result
    }

    private fun buildMvnRepoObject(pageContent: PageContent): MvnRepoDir {
        val rootDirFiles = listOf(ARCHETYPE_CATALOG_NAME, ROBOTS_FILE_NAME)
        val metadataFiles = pageContent.links.filter { it.startsWith(MAVEN_METADATA_FILE) }.toList()
        val versionDirs = pageContent.links.any { it.matches(regexVersionDir) }
        return when {
            pageContent.links.any { rootDirFiles.contains(it) } -> MvnRootDir(url = pageContent.url)
            versionDirs && metadataFiles.isNotEmpty() -> {
                val packageName = pageContent.header.substringBeforeLast("/").replace('/', '.')
                val libraryName = pageContent.header.substringAfterLast("/")
                MvnLibraryDir(
                    url = pageContent.url,
                    groupId = packageName,
                    artifactId = libraryName,
                    metadataUrl = pageContent.url + metadataFiles.first()
                )
            }
            pageContent.links.any { it.endsWith(".pom") || it.endsWith(".jar") } -> {
                val groupNameWithArtifactName = pageContent.header.substringBeforeLast("/")
                val groupName = groupNameWithArtifactName.substringBeforeLast("/").replace('/', '.')
                val artifactName = groupNameWithArtifactName.substringAfterLast("/")
                val version = pageContent.header.substringAfterLast("/")
                MvnVersionDir(
                    url = pageContent.url,
                    groupId = groupName,
                    artifactId = artifactName,
                    version = version
                )
            }
            else -> {
                val groupName = pageContent.header.replace('/', '.')
                MvnGroupDir(url = pageContent.url, groupId = groupName)
            }
        }
    }

    private fun buildMvnSubDirObj(
        repoParentDir: MvnRepoDir,
        entryLink: String,
        entryName: String
    ): MvnRepoDir? {
        // sub folder
        return when (repoParentDir) {
            is MvnRootDir -> MvnGroupDir(url = entryLink, groupId = entryName)
            is MvnGroupDir -> {
                if (repoParentDir is MvnLibraryDir) {
                    MvnVersionDir(
                        url = entryLink,
                        groupId = repoParentDir.groupId,
                        artifactId = repoParentDir.artifactId,
                        version = entryName
                    )
                } else {
                    MvnGroupDir(
                        url = entryLink,
                        groupId = repoParentDir.groupId + "." + entryName
                    )
                }
            }
            is MvnLibraryDir -> {
                MvnVersionDir(
                    url = entryLink,
                    groupId = repoParentDir.groupId,
                    artifactId = repoParentDir.artifactId,
                    version = entryName
                )
            }
            is MvnVersionDir -> null
            else -> null
        }
    }
}
