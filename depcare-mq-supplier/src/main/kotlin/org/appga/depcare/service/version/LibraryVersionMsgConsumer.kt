package org.appga.depcare.service.version

import kotlinx.serialization.DeserializationStrategy
import mu.KLogging
import org.appga.depcare.config.SerializerConfig
import org.appga.depcare.domain.JvmLibraryVersion
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.neo4j.driver.Driver
import org.neo4j.driver.Query
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject

@ApplicationScoped
class LibraryVersionMsgConsumer(
	@Inject
	private val config: SerializerConfig,
	@Inject
	private val driver: Driver
) {
	private val deserializer: DeserializationStrategy<JvmLibraryVersion> = JvmLibraryVersion.serializer()

	private companion object : KLogging()

	@Incoming("in-versions")
	fun process(payload: String) {
		logger.debug { "Consumer: library version message"}
		val jvmLibraryVersion = deserializePayload(payload)
		driver.session().use { session ->
			logger.info { "Adding library version" }
			session.run(
				Query(
					"""
						MERGE (lv:Version {artifactId: ${'$'}artifactId, groupId: ${'$'}groupId, version: ${'$'}version})		
							ON CREATE SET lv.url=${'$'}versionUrl
						MERGE (l:Library {groupId: ${'$'}groupId, artifactId: ${'$'}artifactId})
							ON CREATE set l.url=${'$'}libUrl
						MERGE (lv)-[:VERSION_OF]->(l)
					""".trimIndent()
				).withParameters(mapOf(
					"artifactId" to jvmLibraryVersion.library.artifactId,
					"groupId" to jvmLibraryVersion.library.groupId,
					"version" to jvmLibraryVersion.version,
					"versionUrl" to jvmLibraryVersion.url,
					"libUrl" to jvmLibraryVersion.library.url
				))
			)
		}
	}

	private fun deserializePayload(payload: String): JvmLibraryVersion {
		return config.json.decodeFromString(deserializer, payload)
	}

}