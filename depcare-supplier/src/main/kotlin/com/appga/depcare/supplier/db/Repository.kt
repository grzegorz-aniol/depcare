package com.appga.depcare.db

import com.appga.depcare.domain.JvmLibrary
import com.appga.depcare.domain.JvmLibraryVersion
import com.appga.depcare.domain.LibraryMetadata
import com.appga.depcare.domain.VersionIndication
import mu.KLogging
import org.neo4j.driver.Driver
import org.neo4j.driver.Query
import org.springframework.stereotype.Component

@Component
class Repository(private val driver: Driver) {
    private companion object : KLogging()

    fun saveDependency(
        actualVersion: VersionIndication,
        dependency: VersionIndication
    ) {
        logger.debug { "Adding new dependency" }
        driver.session().use { session ->
            session.run(
                Query(
                    """
						MERGE (lv:Version {artifactId: ${'$'}artifactId, groupId: ${'$'}groupId, version: ${'$'}version}) 
						MERGE (dv: Version {artifactId: ${'$'}depArtifactId, groupId: ${'$'}depGroupId, version: ${'$'}depVersion})
						MERGE (lv)-[d:DEPENDS_ON]->(dv)
							ON CREATE SET d.scope=${'$'}scope, d.isOptional=${'$'}isOptional, d.type=${'$'}type
					""".trimIndent()
                ).withParameters(
                    mapOf(
                        "groupId" to actualVersion.groupId,
                        "artifactId" to actualVersion.artifactId,
                        "version" to actualVersion.version,
                        "depGroupId" to dependency.groupId,
                        "depArtifactId" to dependency.artifactId,
                        "depVersion" to dependency.version,
                        "scope" to dependency.scope,
                        "isOptional" to dependency.optional,
                        "type" to dependency.type,
                    )
                )
            )
        }
    }

    fun saveTransitiveDependency(
        actualVersion: VersionIndication,
        dependency: VersionIndication
    ) {
        logger.debug { "Adding transitive dependency from $actualVersion to $dependency" }
        driver.session().use { session ->
            session.run(
                Query(
                    """
 					    MERGE (v:Version {artifactId: ${'$'}artifactId, groupId: ${'$'}groupId, version: ${'$'}version}) 
 						MERGE (ol:Library {artifactId: ${'$'}depArtifactId, groupId: ${'$'}depGroupId}) 
                        MERGE (v)-[t:TRANS_DEP_ON]->(ol)
                    """.trimIndent()
                ).withParameters(
                    mapOf(
                        "groupId" to actualVersion.groupId,
                        "artifactId" to actualVersion.artifactId,
                        "version" to actualVersion.version,
                        "depGroupId" to dependency.groupId,
                        "depArtifactId" to dependency.artifactId,
                    )
                )
            )
        }
    }

    fun saveParentProject(
        actualVersion: VersionIndication,
        parentVersion: VersionIndication,
    ) {
        logger.debug { "Adding parent relation from $actualVersion to $parentVersion" }
        driver.session().use { session ->
            session.run(
                Query(
                    """
 					    MERGE (v:Version {artifactId: ${'$'}artifactId, groupId: ${'$'}groupId, version: ${'$'}version}) 
 						MERGE (pv:Version {artifactId: ${'$'}parentArtifactId, groupId: ${'$'}parentGroupId, version: ${'$'}parentVersion}) 
                        MERGE (pv)-[t:PARENT_OF]->(v)
                    """.trimIndent()
                ).withParameters(
                    mapOf(
                        "groupId" to actualVersion.groupId,
                        "artifactId" to actualVersion.artifactId,
                        "version" to actualVersion.version,
                        "parentGroupId" to parentVersion.groupId,
                        "parentArtifactId" to parentVersion.artifactId,
                        "parentVersion" to parentVersion.version,
                    )
                )
            )
        }

    }

    fun saveLibrary(jvmLibrary: JvmLibrary, metadata: LibraryMetadata) {
        driver.session().use { session ->
            logger.debug { "Adding library" }
            session.run(
                Query(
                    """
                    MERGE (l:Library {groupId: ${'$'}groupId, artifactId: ${'$'}artifactId})
                        ON CREATE SET 
                            l.name=${'$'}name, l.url=${'$'}url, 
                            l.latestVersion=${'$'}latestVersion, l.releaseVersion=${'$'}releaseVersion, l.lastUpdated=${'$'}lastUpdated, 
                            l.versionsCount=${'$'}versionsCount
                        ON MATCH SET 
                            l.latestVersion=${'$'}latestVersion, l.releaseVersion=${'$'}releaseVersion, l.lastUpdated=${'$'}lastUpdated, 
                            l.versionsCount=${'$'}versionsCount
                    MERGE (g:Group {groupId: ${'$'}groupId})
                    MERGE (l)-[:MEMBER_OF]->(g)
                    """.trimIndent()
                ).withParameters(
                    mapOf(
                        "artifactId" to jvmLibrary.artifactId,
                        "groupId" to jvmLibrary.groupId,
                        "name" to jvmLibrary.name,
                        "url" to jvmLibrary.url,
                        "latestVersion" to metadata.latest,
                        "releaseVersion" to metadata.release,
                        "lastUpdated" to metadata.lastUpdated,
                        "versionsCount" to metadata.versionsCount
                    )
                )
            )
        }
    }

    fun saveLibraryVersion(jvmLibraryVersion: JvmLibraryVersion) {
        driver.session().use { session ->
            logger.info { "Adding library version" }
            session.run(
                Query(
                    """
                        MERGE (lv:Version {artifactId: ${'$'}artifactId, groupId: ${'$'}groupId, version: ${'$'}version})		
                            ON CREATE SET lv.url=${'$'}versionUrl, lv.createdAt=${'$'}createdAt, lv.fileSize=${'$'}fileSize
                            ON MATCH SET lv.url=${'$'}versionUrl, lv.createdAt=${'$'}createdAt, lv.fileSize=${'$'}fileSize
                        MERGE (l:Library {groupId: ${'$'}groupId, artifactId: ${'$'}artifactId})
                            ON CREATE set l.url=${'$'}libUrl
                        MERGE (lv)-[:VERSION_OF]->(l)
                    """.trimIndent()
                ).withParameters(
                    mapOf(
                        "artifactId" to jvmLibraryVersion.library.artifactId,
                        "groupId" to jvmLibraryVersion.library.groupId,
                        "version" to jvmLibraryVersion.version,
                        "versionUrl" to jvmLibraryVersion.url,
                        "libUrl" to jvmLibraryVersion.library.url,
                        "createdAt" to jvmLibraryVersion.createdAt,
                        "fileSize" to jvmLibraryVersion.fileSize
                    )
                )
            )
        }
    }
}
