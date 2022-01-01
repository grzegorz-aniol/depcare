package com.appga.depcare.supplier.service

import com.appga.depcare.supplier.clients.MvnRepoClient
import com.appga.depcare.supplier.db.Repository
import com.ninjasquad.springmockk.MockkBean
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.core.KafkaTemplate

@SpringBootTest
class DependencyAnalyserTest {

	@MockkBean
	lateinit var repository: Repository

	@MockkBean
	lateinit var kafkaTemplate: KafkaTemplate<String, String>

	@Autowired
	lateinit var mvnRepoClient: MvnRepoClient

	@Autowired
	lateinit var underTest: DependencyAnalyser

	@Test
	fun test() {
		every { repository.saveDependency(any(), any()) } just Runs
		every { repository.saveParentProject(any(), any()) } just Runs
		every { repository.saveTransitiveDependency(any(), any()) } just Runs
		underTest.consumer("https://repo1.maven.org/maven2/as/leap/vertx-rpc/3.3.6/vertx-rpc-3.3.6.pom")
	}
}