package org.appga.depcare.service.repo

import io.smallrye.mutiny.Multi
import mu.KotlinLogging
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.eclipse.microprofile.reactive.messaging.Outgoing
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class RepoDirsProcessor(
	private val mavenRepoCrawler: MavenRepoCrawler,
) {
	private val log = KotlinLogging.logger { }

	@Incoming("in-repodirs")
	@Outgoing("out-repodirs")
	fun process(url: String): Multi<String> {
		log.debug { "Processing url message : $url" }
		return Multi.createFrom().iterable(mavenRepoCrawler.analyseRepoDirContent(url).map { it.url })
	}

}