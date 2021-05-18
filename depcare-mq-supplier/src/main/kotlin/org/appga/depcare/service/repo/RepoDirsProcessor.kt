package org.appga.depcare.service.repo

import mu.KotlinLogging
import org.eclipse.microprofile.reactive.messaging.Channel
import org.eclipse.microprofile.reactive.messaging.Emitter
import org.eclipse.microprofile.reactive.messaging.Incoming
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
                payloadEmitter.send(it.url)
            }
        } catch (e: Exception) {
            log.error { "Error: ${e.message}, url: $url" }
        }
    }
}
