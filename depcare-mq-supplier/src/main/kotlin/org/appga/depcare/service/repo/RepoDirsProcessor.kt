package org.appga.depcare.service.repo

import io.smallrye.reactive.messaging.amqp.OutgoingAmqpMetadata
import mu.KotlinLogging
import org.appga.depcare.domain.MvnGroupDir
import org.appga.depcare.domain.MvnLibraryDir
import org.appga.depcare.domain.MvnRepoDir
import org.appga.depcare.domain.MvnRootDir
import org.appga.depcare.domain.MvnVersionDir
import org.eclipse.microprofile.reactive.messaging.Channel
import org.eclipse.microprofile.reactive.messaging.Emitter
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.eclipse.microprofile.reactive.messaging.Message
import org.eclipse.microprofile.reactive.messaging.OnOverflow
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class RepoDirsProcessor(
    private val mavenRepoCrawler: MavenRepoCrawler,
    @Channel("out-repodirs")
    @OnOverflow(value = OnOverflow.Strategy.NONE)
    private val payloadEmitter: Emitter<String>,
) {
    private val log = KotlinLogging.logger { }

    @Incoming("in-repodirs")
    fun process(url: String) {
        log.debug { "Processing url message : $url" }
        try {
            mavenRepoCrawler.analyseRepoDirContent(url).forEach {
                val metadata = OutgoingAmqpMetadata.builder()
                    .withPriority(getMessagePriority(it))
                    .build()
                val message = Message.of(it.url, org.eclipse.microprofile.reactive.messaging.Metadata.of(metadata))
                payloadEmitter.send(message)
            }
        } catch (e: Exception) {
            log.error { "Error: ${e.message}, url: $url" }
        }
    }

    /** The highest message priority is for lower directories.
     * As a result the tree crawler's traversal is BFS */
    fun getMessagePriority(dir: MvnRepoDir): Short {
        return when (dir) {
            is MvnRootDir -> 0
            is MvnGroupDir -> 1
            is MvnLibraryDir -> 2
            is MvnVersionDir -> 3
            else -> 0
        }
    }
}
