package org.appga.depcare.service.version

import kotlinx.serialization.SerializationStrategy
import mu.KLogging
import org.appga.depcare.config.SerializerConfig
import org.appga.depcare.domain.JvmLibraryVersion
import org.eclipse.microprofile.reactive.messaging.Channel
import org.eclipse.microprofile.reactive.messaging.Emitter
import org.eclipse.microprofile.reactive.messaging.OnOverflow
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject

@ApplicationScoped
class LibraryVersionMsgProducer(
	@Channel("out-versions")
	@OnOverflow(value = OnOverflow.Strategy.BUFFER, bufferSize = 10)
	private val payloadEmitter: Emitter<String>,
	@Inject
	private val serializerConfig: SerializerConfig
) {
	private companion object : KLogging()

	private val serializer: SerializationStrategy<JvmLibraryVersion> = JvmLibraryVersion.serializer()

	fun postMessage(obj: JvmLibraryVersion) {
		logger.debug { "Posting new message" }
		val payload = serializerConfig.json.encodeToString(serializer, obj)
		payloadEmitter.send(payload)
	}
}
