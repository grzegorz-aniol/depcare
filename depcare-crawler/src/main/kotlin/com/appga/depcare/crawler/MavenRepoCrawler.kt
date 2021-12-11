package com.appga.depcare.crawler

import com.appga.depcare.domain.JvmLibrary
import com.appga.depcare.domain.JvmLibraryVersion
import com.appga.depcare.domain.MvnGroupDir
import com.appga.depcare.domain.MvnLibraryDir
import com.appga.depcare.domain.MvnRepoDir
import com.appga.depcare.domain.MvnRootDir
import com.appga.depcare.domain.MvnVersionDir
import edu.uci.ics.crawler4j.crawler.Page
import edu.uci.ics.crawler4j.crawler.WebCrawler
import edu.uci.ics.crawler4j.parser.HtmlParseData
import edu.uci.ics.crawler4j.url.WebURL
import mu.KotlinLogging
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

private const val MAVEN_METADATA_FILE = "maven-metadata.xml"
private const val ARCHETYPE_CATALOG_NAME = "archetype-catalog.xml"
private const val ROBOTS_FILE_NAME = "robots.txt"

class MavenRepoCrawler(
	private val pageAnalyzer: PageAnalyzer,
	private val libraryQueueProducer: LibraryQueueProducer,
	private val libraryVersionQueueProducer: LibraryVersionQueueProducer,
) : WebCrawler() {

	private val logger = KotlinLogging.logger {}

	private val urlEndingWithoutFileExtension = Regex("^.+\\/[^\\/^\\.]+\\/?$")

	private val regexIgnoreLinks = Regex(".*(\\.(asc|jar|xml|htm|html|css|php|jpg|jpeg|png|gif))$")

	override fun shouldVisit(referringPage: Page?, url: WebURL?): Boolean {
		val href = url!!.url.toLowerCase()
		val domain = referringPage!!.webURL.domain
		val isAcceptableLink = href.endsWith("/") || !regexIgnoreLinks.matches(href)
		val isSameDomain = href.contains(domain, ignoreCase = true)
		val isSubDir = href.startsWith(referringPage.webURL.url)
		val shouldVisit = isAcceptableLink && isSameDomain && isSubDir
		if (!shouldVisit) {
			logger.debug { "Link won't be visited: $href" }
		}
		return shouldVisit
	}

	override fun shouldFollowLinksIn(url: WebURL?): Boolean {
		return super.shouldFollowLinksIn(url)
	}

	override fun visit(page: Page?) {
		val url = page!!.webURL.url
		logger.info { "Analysing page: $url" }

		if (page.parseData !is HtmlParseData) {
			logger.warn { "Not HTML page $url"}
			return
		}
		val htmlParseData = page.parseData as HtmlParseData
		// val text = htmlParseData.text
		val links = htmlParseData.outgoingUrls
		// logger.debug("Text length: " + text.length)
		logger.debug("Number of outgoing links: " + links.size)
		val pageContent = pageAnalyzer.analyse(htmlParseData.html, url)
		val repoDir = buildMvnRepoObject(pageContent)
		analyseContent(repoDir)

		if (pageContent.links.isEmpty()) {
			logger.warn { "Empty page content for url: ${repoDir.url}" }
		}

		logger.debug { "Page: ${pageContent.url}, header: ${pageContent.header}, links count: ${pageContent.links.size}" }
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
				logger.debug { "Found new library page ${repoDir.url}" }
				libraryQueueProducer.send(library)
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
				logger.debug { "Found new version page ${repoDir.url}"}
				libraryVersionQueueProducer.send(libraryVersion)
			}
			else -> Unit
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