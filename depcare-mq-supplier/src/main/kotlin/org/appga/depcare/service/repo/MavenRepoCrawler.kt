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

private class PageContent(
	val url: String,
	val header: String,
	val links: List<String> = emptyList(),
	val mavenMetadata: MavenMetadata
)
private data class MavenMetadata(val url: String, val isLibrary: Boolean)

private const val MAVEN_METADATA_FILE = "maven-metadata.xml"
private const val ARCHETYPE_CATALOG_NAME = "archetype-catalog.xml"
private const val ROBOTS_FILE_NAME = "robots.txt"

@Dependent
class MavenRepoCrawler(
	@Inject
	private val mvnRepoClient: MvnRepoClient,
	@Inject
	private val libraryProducer : LibraryMsgProducer,
	@Inject
	private val libraryVersionMsgProducer: LibraryVersionMsgProducer,
	@Inject
	private val dependencyAnalyser: DependencyAnalyser
) {
	private companion object : KLogging()

	private val regexLink = Regex("<a href=\"([^\"]+)\"")
	private val regexHeader = Regex("<h1>(.+)</h1>")
	private val totalLibraries = AtomicLong()
	private val totalVersions = AtomicLong()

	// root -> package -> package ... -> library dir -> version dir
	fun analyseRepoDirContent(url: String): List<MvnRepoDir> {
		logger.info { "Analysing repository directory $url" }

		val pageContent = fetchPageContent(url)
		val repoDir = buildMvnRepoObject(pageContent)

		when (repoDir) {
			is MvnLibraryDir -> {
				val cnt = totalLibraries.incrementAndGet()
				logger.info { "Library: ${repoDir.artifactId}, total: $cnt" }
				val library = JvmLibrary(
					name = repoDir.artifactId,
					groupId = repoDir.groupId,
					artifactId = repoDir.artifactId,
					url = repoDir.url
				)
				libraryProducer.postMessage(library)
			}
			is MvnVersionDir -> {
				val cnt = totalVersions.incrementAndGet()
				logger.info { "Library version: ${repoDir.artifactId}, total: $cnt" }
			}
			else -> Unit
		}

		if (pageContent.links.isEmpty()) {
			logger.warn { "Empty page content for url: ${repoDir.url}" }
			return emptyList()
		}

		logger.debug { "Page: ${pageContent.url}, header: ${pageContent.header}, links count: ${pageContent.links
			.size}"}

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
								dependencyAnalyser.postPomLink(repoDir.url + link)
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
												url = repoDir.url
											),
											version = repoDir.version,
											url = repoDir.url + link
										)
										libraryVersionMsgProducer.postMessage(libraryVersion)
									}
									else -> {
										logger.warn { "JAR found in non library folder (${repoDir.url+link})" }
									}
								}
							}
						}
						null
					}
					else -> {
						logger.warn { "Undetected page link: $entryName" }
						null
					}
				}
			}

		logger.debug { "Page crawling result. Number of sub-folders: ${result.size}"}

		return result
	}



	private fun fetchMetadata(url: String): MavenMetadata {
		// TODO: read: lastUpdated, latest, release
		val xmlBody = mvnRepoClient.fetchPage(url)
		val isLibraryDir = xmlBody?.contains("<versioning>")?.and(xmlBody.contains("groupId")) ?: false
		val mvnMetadata = MavenMetadata(url = url, isLibrary = isLibraryDir)
		logger.debug { "Mvn metadata: $mvnMetadata" }
		return mvnMetadata
	}

	private fun fetchPageContent(url: String): PageContent {
		val body = mvnRepoClient.fetchPage(url)

		require(!body.isNullOrEmpty()) { "Empty response" }
		val links = regexLink.findAll(body).mapNotNull { it.groups[1]?.value }.toList()
		if (links.isEmpty()) {
			logger.debug { "Links not found for page: $url" }
		}
		val mavenMetadata = links.firstOrNull { it.startsWith(MAVEN_METADATA_FILE) }?.let { link ->
			val linkUrl = url + link
			fetchMetadata(linkUrl)
		} ?: MavenMetadata(url, false)

		val header = regexHeader.find(body)?.groups?.get(1)?.value
		if (header.isNullOrEmpty()) {
			throw IllegalStateException("Header not found for page: $url")
		}
		return PageContent(
			url = url,
			header = header,
			links = links,
			mavenMetadata = mavenMetadata
		)
	}

	private fun buildMvnRepoObject(pageContent: PageContent): MvnRepoDir {
		val rootDirFiles = listOf(ARCHETYPE_CATALOG_NAME, ROBOTS_FILE_NAME)
		val metadataFiles = pageContent.links.filter { it.startsWith(MAVEN_METADATA_FILE) }.toList()
		return when {
			pageContent.links.any { rootDirFiles.contains(it) } -> MvnRootDir(url = pageContent.url)
			pageContent.mavenMetadata.isLibrary && metadataFiles.isNotEmpty() -> {
				val packageName = pageContent.header.substringBeforeLast("/").replace('/', '.')
				val libraryName = pageContent.header.substringAfterLast("/")
				MvnLibraryDir(url = pageContent.url, groupId = packageName, artifactId = libraryName)
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