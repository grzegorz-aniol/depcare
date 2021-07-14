package com.appga.depcare.service.repo

import mu.KLogging
import com.appga.depcare.domain.JvmLibrary
import com.appga.depcare.domain.JvmLibraryVersion
import com.appga.depcare.domain.MvnGroupDir
import com.appga.depcare.domain.MvnLibraryDir
import com.appga.depcare.domain.MvnRepoDir
import com.appga.depcare.domain.MvnRootDir
import com.appga.depcare.domain.MvnUndetectedDir
import com.appga.depcare.domain.MvnVersionDir
import com.appga.depcare.service.dependency.DependencyAnalyser
import com.appga.depcare.service.library.LibraryMsgProducer
import com.appga.depcare.service.version.LibraryVersionMsgProducer
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
) {
	private companion object : KLogging()

	// root -> package -> package ... -> library dir -> version dir
	fun analyseRepoDirContent(url: String): List<MvnRepoDir> {
		logger.info { "Analysing repository directory $url" }

		val pageContent = pageAnalyzer.fetchPageContent(url)
		val repoDir = buildMvnRepoObject(pageContent)

		analyseContent(repoDir)

		if (pageContent.links.isEmpty()) {
			logger.warn { "Empty page content for url: ${repoDir.url}" }
			return emptyList()
		}

		logger.debug { "Page: ${pageContent.url}, header: ${pageContent.header}, links count: ${pageContent.links.size}" }

		val result = analyzeLinks(pageContent, repoDir)

		logger.debug { "Page crawling result. Number of sub-folders: ${result.size}" }

		return result
	}

	private fun analyseContent(repoDir: MvnRepoDir) {
		when (repoDir) {
			is MvnLibraryDir -> {
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
				val libraryVersion = JvmLibraryVersion(
					library = JvmLibrary(
						name = repoDir.artifactId,
						groupId = repoDir.groupId,
						artifactId = repoDir.artifactId,
						url = repoDir.url,
						metadataUrl = repoDir.url + MAVEN_METADATA_FILE,
					),
					version = repoDir.version,
					url = repoDir.url,
					pomUrl = repoDir.pomUrl,
					jarUrl = repoDir.jarUrl,
					createdAt = repoDir.createdAt,
					fileSize = repoDir.fileSize,
					fileName = repoDir.fileName,
				)
				libraryVersionMsgProducer.postMessage(libraryVersion)
			}
			else -> Unit
		}
	}

	private fun analyzeLinks(
		pageContent: PageContent,
		repoDir: MvnRepoDir
	): List<MvnUndetectedDir> {
		return pageContent.links
			.filter { it != "../" }
			.mapNotNull { link ->
				when {
					!link.contains("..") && link.endsWith("/") -> MvnUndetectedDir(repoDir.url + link)
					else -> null
				}
			}
	}

	private fun buildMvnRepoObject(pageContent: PageContent): MvnRepoDir {
		val rootDirFiles = listOf(ARCHETYPE_CATALOG_NAME, ROBOTS_FILE_NAME)
		val metadataFiles = pageContent.links.filter { it.startsWith(MAVEN_METADATA_FILE) }.toList()
		return when {
			pageContent.links.any { rootDirFiles.contains(it) } -> MvnRootDir(url = pageContent.url)
			metadataFiles.isNotEmpty() -> {
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
				val publicationDateTime = pageContent.files.entries.mapNotNull { it.value.createdAt }.firstOrNull()
				val jarLink = pageContent.findJarLink()
				val pomLink = pageContent.findJarLink()
				val jarFileName = jarLink?.trim('/')
				require(jarLink != null && jarFileName != null) { "Cannot find JAR name in the version folder ${pageContent.url}" }
				val packageFileSize = pageContent.files[jarFileName]?.size
				MvnVersionDir(
					url = pageContent.url,
					pomUrl = pageContent.url + pomLink,
					jarUrl = pageContent.url + jarLink,
					fileName = jarFileName,
					groupId = groupName,
					artifactId = artifactName,
					version = version,
					createdAt = publicationDateTime,
					fileSize = packageFileSize
				)
			}
			else -> {
				val groupName = pageContent.header.replace('/', '.')
				MvnGroupDir(url = pageContent.url, groupId = groupName)
			}
		}
	}

}
