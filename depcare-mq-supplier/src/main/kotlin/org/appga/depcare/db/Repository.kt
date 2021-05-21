package org.appga.depcare.db

import mu.KLogging
import org.appga.depcare.domain.JvmLibrary
import org.appga.depcare.domain.JvmLibraryVersion
import org.appga.depcare.domain.LibraryMetadata
import org.appga.depcare.domain.VersionIndication
import org.neo4j.driver.Driver
import org.neo4j.driver.Query
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject

@ApplicationScoped
class Repository(
    @Inject
    private val driver: Driver
) {
    private companion object : KLogging()

    fun saveDependency(
        actualVersionIndication: VersionIndication,
        depIndication: VersionIndication
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
                        "groupId" to actualVersionIndication.group,
                        "artifactId" to actualVersionIndication.artifact,
                        "version" to actualVersionIndication.version,
                        "depGroupId" to depIndication.group,
                        "depArtifactId" to depIndication.artifact,
                        "depVersion" to depIndication.version,
                        "scope" to depIndication.scope,
                        "isOptional" to depIndication.optional,
                        "type" to depIndication.type,
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
                            ON CREATE SET lv.url=${'$'}versionUrl, lv.createdAt=${'$'}createdAt
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
                    )
                )
            )
        }
    }
}
