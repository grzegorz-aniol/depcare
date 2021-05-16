package org.appga.depcare.service.library

import kotlinx.serialization.DeserializationStrategy
import mu.KLogging
import org.appga.depcare.config.SerializerConfig
import org.appga.depcare.domain.JvmLibrary
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.neo4j.driver.Driver
import org.neo4j.driver.Query
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject

@ApplicationScoped
class LibraryMsgConsumer(
	@Inject
	private val config: SerializerConfig,
	@Inject
	private val driver: Driver
) {
	private companion object : KLogging()

	private val deserializer: DeserializationStrategy<JvmLibrary> = JvmLibrary.serializer()

	@Incoming("in-libs")
	fun process(payload: String) {
		logger.debug { "Consumer: library message"}
		val jvmLibrary = deserializePayload(payload)
		driver.session().use { session ->
			logger.info { "Adding library" }
			session.run(Query(
				"""
				MERGE (l:Library {groupId: ${'$'}groupId, artifactId: ${'$'}artifactId})
					ON CREATE SET l.name=${'$'}name, l.url=${'$'}url
				MERGE (g:Group {groupId: ${'$'}groupId})
				MERGE (l)-[:MEMBER_OF]->(g)
				""".trimIndent()
			).withParameters(mapOf(
				"artifactId" to jvmLibrary.artifactId,
				"groupId" to jvmLibrary.groupId,
				"name" to jvmLibrary.name,
				"url" to jvmLibrary.url
			)))
		}
	}

	private fun deserializePayload(payload: String): JvmLibrary {
		return config.json.decodeFromString(deserializer, payload)
	}

}