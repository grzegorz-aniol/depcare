package com.appga.depcare.crawler

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class PageAnalyzerTest {

	private val pageAnalyzer = PageAnalyzer()

	@Test
	fun `analyze page from jcentral maven repository`() {
		// given
		val page = mavenRepoPage

		// when
		val result = pageAnalyzer.analyse(page, "https://repo.maven.apache.org/maven2/be/adaxisoft/Bencode/")

		// then
		assertThat(result.links).hasSize(5)
		assertThat(result.header).isNotEmpty()
		assertThat(result.files).hasSize(3)
	}

	@Test
	fun `analyze page from spring maven repository`() {
		// given
		val page = springRepoPage

		// when
		val result = pageAnalyzer.analyse(page, url = "https://repo.spring.io/artifactory/libs-release/io/pivotal/cfenv/java-cfenv")

		// then
		assertThat(result.links).hasSize(20)
		assertThat(result.header).isNotEmpty()
		assertThat(result.files).hasSize(1)
	}

	private val mavenRepoPage =
		"""<!DOCTYPE html>
			<html>		
			<head>
				<title>Central Repository: be/adaxisoft/Bencode</title>
				<meta name="viewport" content="width=device-width, initial-scale=1.0">
				<style>
			body {
				background: #fff;
			}
				</style>
			</head>
			
			<body>
				<header>
					<h1>be/adaxisoft/Bencode</h1>
				</header>
				<hr/>
				<main>
					<pre id="contents">
			<a href="../">../</a>
			<a href="1.0.0/" title="1.0.0/">1.0.0/</a>                                            2016-06-21 19:14         -      
			<a href="2.0.0/" title="2.0.0/">2.0.0/</a>                                            2016-06-21 19:33         -      
			<a href="maven-metadata.xml" title="maven-metadata.xml">maven-metadata.xml</a>                                2016-06-21 19:33       357      
			<a href="maven-metadata.xml.md5" title="maven-metadata.xml.md5">maven-metadata.xml.md5</a>                            2016-06-21 19:33        32      
			<a href="maven-metadata.xml.sha1" title="maven-metadata.xml.sha1">maven-metadata.xml.sha1</a>                           2016-06-21 19:33        40      
					</pre>
				</main>
				<hr/>
			</body>
			
			</html>	
		""".trimIndent()

	private val springRepoPage =
		"""<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 3.2 Final//EN">
			<html>
			<head><meta name="robots" content="noindex" />
			<title>Index of libs-release/io/pivotal/cfenv/java-cfenv</title>
			</head>
			<body>
			<h1>Index of libs-release/io/pivotal/cfenv/java-cfenv</h1>
			<pre>Name                Last modified      Size</pre><hr/>
			<pre><a href="../">../</a>
			<a href="1.0.0.RELEASE/">1.0.0.RELEASE/</a>       01-Mar-2019 19:59    -
			<a href="1.0.1.RELEASE/">1.0.1.RELEASE/</a>       05-Mar-2019 21:24    -
			<a href="1.1.0.RELEASE/">1.1.0.RELEASE/</a>       18-Jun-2019 18:27    -
			<a href="1.1.1.RELEASE/">1.1.1.RELEASE/</a>       07-Aug-2019 17:07    -
			<a href="1.1.2.RELEASE/">1.1.2.RELEASE/</a>       08-Nov-2019 20:12    -
			<a href="1.1.3.RELEASE/">1.1.3.RELEASE/</a>       15-Nov-2019 20:19    -
			<a href="2.0.0.RELEASE/">2.0.0.RELEASE/</a>       18-Nov-2019 14:51    -
			<a href="2.0.1.RELEASE/">2.0.1.RELEASE/</a>       02-Dec-2019 15:48    -
			<a href="2.1.0.RELEASE/">2.1.0.RELEASE/</a>       03-Dec-2019 15:45    -
			<a href="2.1.1.RELEASE/">2.1.1.RELEASE/</a>       22-Jan-2020 15:46    -
			<a href="2.1.2.RELEASE/">2.1.2.RELEASE/</a>       31-Mar-2020 20:34    -
			<a href="2.2.0.RELEASE/">2.2.0.RELEASE/</a>       30-Jun-2020 16:11    -
			<a href="2.2.1.RELEASE/">2.2.1.RELEASE/</a>       07-Jul-2020 18:58    -
			<a href="2.2.2.RELEASE/">2.2.2.RELEASE/</a>       09-Jul-2020 17:48    -
			<a href="2.2.3.RELEASE/">2.2.3.RELEASE/</a>       11-Nov-2020 20:05    -
			<a href="2.2.4.RELEASE/">2.2.4.RELEASE/</a>       02-Dec-2020 16:12    -
			<a href="2.2.5.RELEASE/">2.2.5.RELEASE/</a>       27-Jan-2021 19:42    -
			<a href="2.3.0/">2.3.0/</a>               05-Mar-2021 19:52    -
			<a href="2.4.0/">2.4.0/</a>               09-Jul-2021 17:34    -
			<a href="maven-metadata.xml">maven-metadata.xml</a>   12-Jul-2021 14:16  1.05 KB
			</pre>
			<hr/><address style="font-size:small;">Artifactory Online Server at repo.spring.io Port 80</address></body></html>			
		""".trimIndent()
}