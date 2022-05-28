package com.appga.depcare.supplier.service

import com.appga.depcare.domain.JvmLibraryVersion
import com.appga.depcare.domain.VersionIndication
import com.appga.depcare.supplier.clients.MvnRepoClient
import com.appga.depcare.supplier.db.Repository
import com.appga.depcare.supplier.utils.forEach
import com.appga.depcare.supplier.utils.getFirstElement
import com.appga.depcare.supplier.utils.getFirstElementValue
import com.appga.depcare.supplier.utils.getTextValue
import com.google.common.annotations.VisibleForTesting
import mu.KLogging
import org.springframework.stereotype.Service
import org.w3c.dom.Element

@Service
class DependencyAnalyser(
	private val mvnRepoClient: MvnRepoClient,
	private val repository: Repository
) {
	private companion object : KLogging()

	private data class PomVersionIndications(val parent: VersionIndication?, val project: VersionIndication)

	@VisibleForTesting
	fun saveVersionWithDependencies(libraryVersion: JvmLibraryVersion) {
		repository.saveLibraryVersion(libraryVersion)
		val url = libraryVersion.pomUrl
		if (url != null) {
			analyzePom(url, libraryVersion.library.groupId, libraryVersion.library.artifactId, libraryVersion.version)
		}
	}

	fun analyzePom(url: String, groupId: String, artifactId: String, version: String) {
		logger.info { "Fetching version POM for $url" }
		val doc = mvnRepoClient.fetchXmlDocument(url)

		val rootElement = doc.documentElement
		if (rootElement == null) {
			logger.error { "Missing root xml element" }
			return
		}

		var pomVersions = getPomVersionIndications(rootElement)

		val projectProperties = getPomProperties(rootElement, pomVersions)

		pomVersions = resolvePropertiesInVersionIndication(pomVersions, projectProperties)

		val actualVersionIndication = mergeVersionIndications(pomVersions.parent, pomVersions.project, version)
		if (!actualVersionIndication.isValid()) {
			logger.error { "Cannot find library group, artifact or version in POM xml. Url: $url" }
			return
		}
		if (actualVersionIndication.groupId != groupId ||
			actualVersionIndication.artifactId != artifactId ||
			actualVersionIndication.version != version) {
			logger.warn { "Library version from POM $actualVersionIndication is different than in repository $groupId:$artifactId:$version" }
		}

		projectProperties.setProjectVersion(pomVersions.parent?.version, actualVersionIndication.version ?: version)

		pomVersions.parent?.takeIf { it.isValid() }?.let { parentVersion ->
			repository.saveParentProject(actualVersionIndication, parentVersion)
		}

		rootElement.getFirstElement("dependencies")?.let { dependencies ->
			dependencies.getElementsByTagName("dependency")?.forEach { dep ->
				val depIndication = getDependencyIndication(projectProperties, dep)
				if (depIndication.isValid()) {
					// Version may not be specified. In such case the right dependency version should be determined by analyzing the parent POM
					if (depIndication.hasVersion()) {
						repository.saveDependency(actualVersion = actualVersionIndication, dependency = depIndication)
					} else {
						repository.saveTransitiveDependency(actualVersion = actualVersionIndication, dependency = depIndication)
					}
				} else {
					logger.error { "Cannot find library dependency group, artifact or version ($url)" }
				}
			}
		}
	}

	private fun getPomProperties(rootElement: Element, pomVersions: PomVersionIndications): ProjectProperties {
		// read and resolve properties
		val projectProperties = ProjectProperties(
			parentVersion = pomVersions.parent?.version ?: "",
			projectVersion = pomVersions.project.version
		)
		rootElement.getFirstElement("properties")?.run {
			this.forEach { projectProperties.add(key = it.tagName, value = it.getTextValue() ?: "") }
		}
		return projectProperties
	}

	private fun getPomVersionIndications(rootElement: Element): PomVersionIndications {
		// get original project and parent project indication
		val parentVersionIndication = rootElement.getFirstElement("parent")?.let { getVersionIndication(it) }
		val thisVersionIndication = getVersionIndication(rootElement)

		return PomVersionIndications(
			parent = parentVersionIndication,
			project = thisVersionIndication,
		)
	}

	private fun getVersionIndication(element: Element): VersionIndication {
		return VersionIndication(
			element.getFirstElementValue("groupId"),
			element.getFirstElementValue("artifactId"),
			element.getFirstElementValue("version"),
		)
	}

	private fun getVersionIndication(projectProperties: ProjectProperties, element: Element): VersionIndication {
		return VersionIndication(
			element.getFirstElementValue("groupId"),
			element.getFirstElementValue("artifactId"),
			projectProperties.resolve(element.getFirstElementValue("version")),
		)
	}

	private fun getDependencyIndication(projectProperties: ProjectProperties, element: Element): VersionIndication {
		return VersionIndication(
			element.getFirstElementValue("groupId"),
			element.getFirstElementValue("artifactId"),
			projectProperties.resolve(element.getFirstElementValue("version")),
			element.getFirstElementValue("scope"),
			element.getFirstElementValue("optional")?.let { it == "true" },
			element.getFirstElementValue("type"),
		)
	}

	private fun mergeVersionIndications(parent: VersionIndication?, current: VersionIndication?, expected: String): VersionIndication {
		return VersionIndication(
			groupId = current?.groupId?.takeIf { it.isNotBlank() } ?: parent?.groupId,
			artifactId = current?.artifactId,
			version = current?.version?.takeIf { it.isNotBlank() } ?: parent?.version?.takeIf { it.isNotBlank() } ?: expected
		)
	}

	private fun resolvePropertiesInVersionIndication(pomVersionIndications: PomVersionIndications, projectProperties: ProjectProperties): PomVersionIndications {
		return PomVersionIndications(
			parent = pomVersionIndications.parent?.let { resolveVersionIndication(it, projectProperties) },
			project = resolveVersionIndication(pomVersionIndications.project, projectProperties)
		)
	}

	private fun resolveVersionIndication(versionIndication: VersionIndication, projectProperties: ProjectProperties): VersionIndication {
		val resolvedValue = projectProperties.resolve(versionIndication.version)
		return versionIndication.copy(version = resolvedValue)
	}
}
