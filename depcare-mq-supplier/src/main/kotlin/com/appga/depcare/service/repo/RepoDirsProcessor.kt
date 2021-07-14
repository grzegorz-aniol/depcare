package com.appga.depcare.service.repo

import io.smallrye.reactive.messaging.amqp.OutgoingAmqpMetadata
import mu.KotlinLogging
import com.appga.depcare.domain.MvnGroupDir
import com.appga.depcare.domain.MvnLibraryDir
import com.appga.depcare.domain.MvnRepoDir
import com.appga.depcare.domain.MvnRootDir
import com.appga.depcare.domain.MvnVersionDir
import org.eclipse.microprofile.reactive.messaging.Channel
import org.eclipse.microprofile.reactive.messaging.Emitter
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.eclipse.microprofile.reactive.messaging.Message
import org.eclipse.microprofile.reactive.messaging.OnOverflow
import java.lang.Integer.max
import java.lang.Integer.min
import javax.enterprise.context.ApplicationScoped
import javax.enterprise.context.Dependent

@Dependent
class RepoDirsProcessor(
    private val mavenRepoCrawler: MavenRepoCrawler,
    @Channel("out-repodirs")
    @OnOverflow(value = OnOverflow.Strategy.NONE)
    private val payloadEmitter: Emitter<String>,
) {
    private val log = KotlinLogging.logger { }

    @Incoming("in-repodirs")
    fun process(url: String) {
        log.info { "Processing by ${this}"}
        log.debug { "Processing url message : $url" }
        try {
            mavenRepoCrawler.analyseRepoDirContent(url).forEach {
                val metadata = OutgoingAmqpMetadata.builder()
                    .withPriority(getMessagePriority(it).toShort())
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
    fun getMessagePriority(dir: MvnRepoDir): Int {
        return min(max(0, dir.level), 9)
    }
}