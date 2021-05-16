package org.appga.depcare.rest

import mu.KLogging
import org.appga.depcare.service.repo.RepoDirsProcessor
import java.net.URL
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.core.MediaType

@Path("/api/repodirs")
class RestControllers(
	private val repoDirsProducer: RepoDirsProcessor
) {
	private companion object : KLogging()

	@POST
	@Consumes(MediaType.TEXT_PLAIN)
	fun addNewRepoDir(body: String) {
		val url = URL(body)
		repoDirsProducer.process(url.toString())
	}

}
