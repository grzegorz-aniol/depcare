package org.appga.depcaresuplier

import mu.KotlinLogging
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URL
import java.util.concurrent.atomic.AtomicLong

data class MavenRepo(val name: String, val rootPath: URL)

data class LibraryLocation(val url: URL)

data class LibraryVersionLocation(val url: URL)

data class JvmLibrary(val name: String, val groupId: String, val artifactId: String, val packageName: String)

data class JvmLibraryVersion(
	val library: JvmLibrary,
	val version: String,
	val locations: List<LibraryVersionLocation>
)

operator fun URL.plus(path: String): URL {
	return URL(this.toString().trim('/') + "/" + path.trim('/'))
}

sealed class MavenRepoFolder(val url: URL)

class MvnRepoRootDir(url: URL) : MavenRepoFolder(url)
class MvnRepoPackageDir(url: URL, val packageName: String) : MavenRepoFolder(url)
class MvnLibraryDir(url: URL, val packageName: String, val libraryName: String) : MavenRepoFolder(url)
class MvnVersionDir(url: URL, val library: JvmLibrary) : MavenRepoFolder(url)

class MavenDepCrawler(
	val mavenRepo: MavenRepo,
	val startingFolder: MavenRepoFolder? = MvnRepoRootDir(mavenRepo.rootPath),
) {
	private val log = KotlinLogging.logger { }
	private val httpClient = OkHttpClient()
	private val linksRegexp = Regex("<a href=\"([^\"]+)\"")
	private val versionRegexp = Regex(".*\\d+\\.\\d+")
	private val totalRequests = AtomicLong()
	private val totalLibraries = AtomicLong()
	private val totalVersions = AtomicLong()

	// root -> package -> package ... -> library dir -> version dir
	fun getFolderContent(repoFolder: MavenRepoFolder) {
		when (repoFolder) {
			is MvnLibraryDir -> {
				val cnt = totalLibraries.incrementAndGet()
				log.info { "Library: ${repoFolder.libraryName}, total: $cnt" }
			}
			is MvnVersionDir -> {
				val cnt = totalVersions.incrementAndGet()
				log.info { "Library version: ${repoFolder.library.artifactId}, total: $cnt" }
			}
		}

		val request = Request.Builder()
			.url(repoFolder.url)
			.addHeader("Connection", "keep-alive")
			.get()
			.build()

		log.debug { "Request: ${repoFolder.url}" }

		httpClient.newCall(request).execute().use { response ->
			log.debug { "Response: ${response.code}" }
			if (response.code > 400) {
				throw IllegalStateException("Failed request: ${repoFolder.url}")
			}
			totalRequests.incrementAndGet()
			val body = response.body?.string()

			require(!body.isNullOrEmpty()) { "Empty response" }
			val links = linksRegexp.findAll(body).mapNotNull { it.groups[1]?.value }.toList()

			if (links.isEmpty()) {
				log.debug { "Url: ${repoFolder.url}. Links not found" }
				return
			}

			val metadataFiles = links.filter { it.startsWith("maven-metadata.xml") }.toList()
			val isLibraryDir = metadataFiles.isNotEmpty()
			val isLibraryVersionDir = !isLibraryDir && links.any { it.endsWith(".pom") || it.endsWith(".jar") }

			links.filter { it != "../" }
				.forEach { link ->
					val entryName = link.substringBeforeLast("/")
					val entryLink = repoFolder.url + link
					when {
						link.endsWith("/") -> {
							// sub folder
							val newSubFolder = when (repoFolder) {
								is MvnRepoRootDir -> MvnRepoPackageDir(url = entryLink, packageName = entryName)
								is MvnRepoPackageDir -> {
									if (!isLibraryDir) {
										MvnRepoPackageDir(
											url = entryLink,
											packageName = repoFolder.packageName + "." + entryName
										)
									} else {
										null
//										val library = JvmDependency(
//											name = repoFolder.packageName, // get from XML
//											groupId = repoFolder.packageName.substringBeforeLast("."),
//											artifactId = repoFolder.packageName.substringAfterLast("."),
//											packageName = repoFolder.packageName
//										)
//										MvnVersionDir(
//											url = entryLink,
//											library = library
//										)
									}
								}
								is MvnLibraryDir -> {
//									val library = JvmDependency(
//										name = repoFolder.packageName, // get from XML
//										groupId = repoFolder.packageName.substringBeforeLast("."),
//										artifactId = repoFolder.packageName.substringAfterLast("."),
//										packageName = repoFolder.packageName
//									)
//									MvnVersionDir(
//										url = entryLink,
//										library = library
//									)
									null
								}
								is MvnVersionDir -> null
							}
							if (newSubFolder != null) {
								getFolderContent(newSubFolder)
							}
						}
						entryName != ".." -> {
							when {
								link.endsWith(".pom") -> {
									log.info { "Library POM: $entryName" }
								}
								link.endsWith(".jar") && !link.contains("javadoc") && !link.contains("sources") -> {
									log.info { "Library JAR: $entryName" }
								}
							}
						}
					}
				}
		}
	}
}


fun main(args: Array<String>) {
	val mavenCentralUrl = URL("https://repo1.maven.org/maven2/")
	val mavenCentralRepo = MavenRepo("Central", mavenCentralUrl)
	val crawler = MavenDepCrawler(mavenCentralRepo)
	crawler.getFolderContent(MvnRepoRootDir(mavenCentralUrl))
}
