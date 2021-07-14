package com.appga.depcaresuplier.service

import kotlinx.serialization.json.Json
import mu.KotlinLogging
import com.appga.depcare.domain.MavenRepo
import com.appga.depcare.domain.MvnRepoDir
import com.appga.depcare.domain.MvnRootDir
import org.springframework.jms.annotation.JmsListener
import org.springframework.jms.core.JmsTemplate
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service
import java.net.URL

@Service
class DirectoryQueueService(
	private val jmsTemplate: JmsTemplate,
	private val json: Json
) {
	private val log = KotlinLogging.logger { }
	private val mavenCentralUrl = URL("https://repo1.maven.org/maven2/")
	private val mavenCentralRepo = MavenRepo("Central", mavenCentralUrl.toString())
	private val mvnDir = MvnRootDir(mavenCentralUrl.toString())

	fun addFolder(dir: String) {
		val mvnRootDir = MvnRootDir(mvnDir.url)
		log.info { "New folder ${mvnRootDir.url} added to queue"}
		addToQueue(mvnRootDir)
	}

	private fun addToQueue(mvnDir: MvnRepoDir) {
		val payload = json.encodeToString(MvnRepoDir.serializer(), mvnDir)
		jmsTemplate.convertAndSend("dirs", payload as Any)
	}

	@JmsListener(destination = "dirs")
	fun consumeMessage(@Payload payload: String) {
		val mvnDir = json.decodeFromString(MvnRepoDir.serializer(), payload)
//		val newSubDirs = mavenDepCrawler.getDirContent(mvnDir)
//		newSubDirs.forEach { subDir ->
//			addToQueue(subDir)
//		}
	}
}