package com.appga.depcare.crawler

import com.appga.depcare.crawler.analyzers.MavenAnalyzer
import com.appga.depcare.crawler.analyzers.PageAnalyzer
import com.appga.depcare.crawler.kafka.LibraryQueueProducer
import com.appga.depcare.crawler.kafka.LibraryVersionQueueProducer
import com.appga.depcare.crawler.metrics.MetricsService
import com.appga.depcare.domain.JvmLibraryVersion
import edu.uci.ics.crawler4j.crawler.CrawlConfig
import edu.uci.ics.crawler4j.crawler.Page
import edu.uci.ics.crawler4j.parser.Parser
import edu.uci.ics.crawler4j.url.WebURL
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.SpyK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant
import kotlin.math.roundToLong

//@SpringBootTest
@ExtendWith(MockKExtension::class)
internal class MavenAnalyzerTest {

//	@MockkBean
	@SpyK
	private var pageAnalyzer: PageAnalyzer = PageAnalyzer()

//	@MockkBean
	@MockK
	private lateinit var libraryQueueProducer: LibraryQueueProducer

//	@MockkBean
	@MockK
	private lateinit var libraryVersionQueueProducer: LibraryVersionQueueProducer

//	@MockkBean
	@MockK
	private lateinit var metricsService: MetricsService

//	@Autowired
	@InjectMockKs
	lateinit var mavenAnalyzer: MavenAnalyzer

	@Test
	fun shouldGenerateProperGroupId() {
		// given
		val url = "https://repo.spring.io/artifactory/libs-release/org/springframework/boot/spring-boot-starter/2.5.5/"
		val title = "Index of libs-release/org/springframework/boot/spring-boot-starter/2.5.5"
		val page = Page(WebURL().also { it.url = url })
		val slotLibraryVersion = slot<JvmLibraryVersion>()
		every { libraryVersionQueueProducer.send(capture(slotLibraryVersion))} just runs
		every { metricsService.tickPagesCounter() } just runs

		// when
		page.load(StringEntity(SPRING_BOOT_STARTER_2_5_5, ContentType.TEXT_HTML), Int.MAX_VALUE)
		val parser = Parser(CrawlConfig())
		parser.parse(page, url)
		mavenAnalyzer.visit(page)

		// then
		assertThat(slotLibraryVersion.isCaptured).isTrue
		assertThat(slotLibraryVersion.captured.library.artifactId).isEqualTo("spring-boot-starter")
		assertThat(slotLibraryVersion.captured.library.groupId).isEqualTo("org.springframework.boot")
		assertThat(slotLibraryVersion.captured.approxFileSize).isEqualTo((4.66 * 1024).roundToLong())
		assertThat(slotLibraryVersion.captured.publishedAt).isEqualTo(Instant.parse("2021-09-23T07:44:00.000Z"))
		verify(exactly = 1) {
			libraryVersionQueueProducer.send(any())
		}
		verify(exactly = 0) {
			libraryQueueProducer.send(any())
		}
	}

	private val SPRING_BOOT_STARTER_2_5_5 =
	"""
		<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 3.2 Final//EN">
		<html>
		<head><meta name="robots" content="noindex" />
		<title>Index of libs-release/org/springframework/boot/spring-boot-starter/2.5.5</title>
		</head>
		<body>
		<h1>Index of libs-release/org/springframework/boot/spring-boot-starter/2.5.5</h1>
		<pre>Name                                       Last modified      Size</pre><hr/>
		<pre><a href="../">../</a>
		<a href="spring-boot-starter-2.5.5-javadoc.jar">spring-boot-starter-2.5.5-javadoc.jar</a>       23-Sep-2021 07:44  4.63 KB
		<a href="spring-boot-starter-2.5.5-javadoc.jar.asc">spring-boot-starter-2.5.5-javadoc.jar.asc</a>   23-Sep-2021 07:45  488 bytes
		<a href="spring-boot-starter-2.5.5-sources.jar">spring-boot-starter-2.5.5-sources.jar</a>       23-Sep-2021 07:44  4.63 KB
		<a href="spring-boot-starter-2.5.5-sources.jar.asc">spring-boot-starter-2.5.5-sources.jar.asc</a>   23-Sep-2021 07:45  488 bytes
		<a href="spring-boot-starter-2.5.5.jar">spring-boot-starter-2.5.5.jar</a>               23-Sep-2021 07:44  4.66 KB
		<a href="spring-boot-starter-2.5.5.jar.asc">spring-boot-starter-2.5.5.jar.asc</a>           23-Sep-2021 07:45  488 bytes
		<a href="spring-boot-starter-2.5.5.module">spring-boot-starter-2.5.5.module</a>            23-Sep-2021 07:44  7.57 KB
		<a href="spring-boot-starter-2.5.5.module.asc">spring-boot-starter-2.5.5.module.asc</a>        23-Sep-2021 07:45  488 bytes
		<a href="spring-boot-starter-2.5.5.pom">spring-boot-starter-2.5.5.pom</a>               23-Sep-2021 07:44  2.99 KB
		<a href="spring-boot-starter-2.5.5.pom.asc">spring-boot-starter-2.5.5.pom.asc</a>           23-Sep-2021 07:45  488 bytes
		</pre>
		<hr/><address style="font-size:small;">Artifactory Online Server at repo.spring.io Port 80</address></body></html>     
	""".trimIndent()
}