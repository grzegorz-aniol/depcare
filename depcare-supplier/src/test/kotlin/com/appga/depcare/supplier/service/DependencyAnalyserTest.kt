package com.appga.depcare.supplier.service

import com.appga.depcare.domain.VersionIndication
import com.appga.depcare.supplier.clients.MvnRepoClient
import com.appga.depcare.supplier.db.Repository
import com.ninjasquad.springmockk.MockkBean
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.verify
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
	lateinit var dependencyAnalyser: DependencyAnalyser

	@Test
	fun test() {
		every { repository.saveDependency(any(), any()) } just Runs
		every { repository.saveParentProject(any(), any()) } just Runs
		every { repository.saveTransitiveDependency(any(), any()) } just Runs
		dependencyAnalyser.consumer("https://repo1.maven.org/maven2/as/leap/vertx-rpc/3.3.6/vertx-rpc-3.3.6.pom")
	}

	@Test
	fun `properties should be resolved`() {
		// given
		every { repository.saveDependency(any(), any()) } just Runs
		every { repository.saveParentProject(
			actualVersion = VersionIndication(
				groupId = "ai.catboost",
				artifactId = "catboost-spark_2.4_2.12",
				version = "0.25"
			),
			parentVersion = VersionIndication(
				groupId = "ai.catboost",
				artifactId = "catboost-spark-aggregate_2.12",
				version = "0.25"
			)
		) } just Runs
		every { repository.saveTransitiveDependency(any(), any()) } just Runs

		// when
		dependencyAnalyser.consumer("https://repo1.maven.org/maven2/ai/catboost/catboost-spark_2.4_2.12/0.25/catboost-spark_2.4_2.12-0.25.pom")

		// then
		verify(exactly = 1) { repository.saveDependency(
			actualVersion = any(),
			dependency = VersionIndication(
				groupId = "ai.catboost",
				artifactId = "catboost-common",
				version = "0.25"
			)
		) }
	}
}