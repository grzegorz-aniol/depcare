package com.appga.depcare.domain

enum class MavenRepoMetadata(val url: String, val rootPath: String) {
	MAVEN_ORG("https://repo1.maven.org/maven2/", "/maven2"),
	SPRING_IO("https://repo.spring.io/artifactory/libs-release/", "/artifactory/libs-release");

	companion object {
		fun findByUrl(url: String): MavenRepoMetadata? =
			MavenRepoMetadata.values().firstOrNull { url.startsWith(it.url, ignoreCase = true) }
	}
}