package com.appga.depcare.supplier.kafka

import com.appga.depcare.domain.JvmLibrary
import com.appga.depcare.domain.LibraryMetadata
import com.appga.depcare.supplier.clients.MvnRepoClient
import com.appga.depcare.supplier.configuration.KafkaTopics
import com.appga.depcare.supplier.configuration.SerializerConfiguration
import com.appga.depcare.supplier.db.Repository
import com.appga.depcare.supplier.utils.asSequence
import com.appga.depcare.supplier.utils.getFirstElement
import com.appga.depcare.supplier.utils.getFirstElementValue
import kotlinx.serialization.DeserializationStrategy
import mu.KLogging
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Component
class LibraryMsgConsumer(
	private val config: SerializerConfiguration,
	private val mvnRepoClient: MvnRepoClient,
	private val repository: Repository
) {
	private companion object : KLogging()

	private val deserializer: DeserializationStrategy<JvmLibrary> = JvmLibrary.serializer()

	@KafkaListener(topics = [KafkaTopics.TOPIC_LIBS])
	fun process(@Payload payload: String) {
		logger.info { "Consumer: library message" }
		try {
			val jvmLibrary = deserializePayload(payload)
			val metadata = fetchMetaFile(jvmLibrary)
			repository.saveLibrary(jvmLibrary, metadata)
		} catch (ex: Exception) {
			logger.error("Error processing library payload: $payload", ex.message)
		}
	}

	private fun deserializePayload(payload: String): JvmLibrary {
		return config.jsonSerializer().decodeFromString(deserializer, payload)
	}

	private fun fetchMetaFile(jvmLibrary: JvmLibrary): LibraryMetadata {
		logger.debug { "Fetching meta file ${jvmLibrary.metadataUrl}" }
		val document = mvnRepoClient.fetchXmlDocument(jvmLibrary.metadataUrl)
		var lastUpdatedText: String? = null
		var latest: String? = null
		var release: String? = null
		var versionsCount: Int? = null
		var lastUpdated: LocalDateTime? = null
		document.documentElement.run {
			getFirstElement("versioning")?.let {
				latest = getFirstElementValue("latest")
				release = getFirstElementValue("release")
				lastUpdatedText = getFirstElementValue("lastUpdated")
				getFirstElement("versions")?.let {
					versionsCount = getElementsByTagName("version").asSequence().count()
				}
			}
		}
		if (lastUpdatedText != null) {
			val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
			lastUpdated = LocalDateTime.parse(lastUpdatedText, formatter)
		}
		return LibraryMetadata(
			latest = latest,
			release = release,
			lastUpdated = lastUpdated,
			versionsCount = versionsCount
		)
	}
}
