package com.appga.depcare.supplier.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test


internal class ProjectPropertiesTest {

	@Test
	fun `should resolve parent version`() {
		// given
		val projectProperties = ProjectProperties(parentVersion = "1.2", projectVersion = "\${parent.version}")
		// when
		val projectVersion = projectProperties.resolve("\${project.version}")
		val parentVersion = projectProperties.resolve("\${parent.version}")
		val parentVersion2 = projectProperties.resolve("\${parent.project.version}")
		// then
		assertThat(projectVersion).isEqualTo("1.2")
		assertThat(parentVersion).isEqualTo("1.2")
		assertThat(parentVersion2).isEqualTo("1.2")
	}

	@Test
	fun `should resolve project version in other variable value`() {
		// given
		val projectProperties = ProjectProperties(parentVersion = "1.2", projectVersion = "2.10")
		projectProperties.add("module.version", "\${project.version}")
		projectProperties.add("module2.version", "\${version}")
		// when
		val module1 = projectProperties.resolve("\${module.version}")
		val module2 = projectProperties.resolve("\${module2.version}")
		// then
		assertThat(module1).isEqualTo("2.10")
		assertThat(module2).isEqualTo("2.10")
	}

	@Test
	fun `should resolve variable`() {
		// given
		val projectProperties = ProjectProperties(parentVersion = "1.2", projectVersion = "2.10")
		projectProperties.add("module.version", "2.15")
		projectProperties.add("module2.version", "10.2")
		// when
		val module1 = projectProperties.resolve("\${module.version}")
		val module2 = projectProperties.resolve("\${module2.version}")
		// then
		assertThat(module1).isEqualTo("2.15")
		assertThat(module2).isEqualTo("10.2")
	}

	@Test
	fun `should resolve missing variable`() {
		// given
		val projectProperties = ProjectProperties(parentVersion = "1.2", projectVersion = "2.10")
		// when
		val module1 = projectProperties.resolve("\${module.version}")
		// then
		assertThat(module1).isEqualTo("\${module.version}")
	}

	@Test
	fun `should resolve recursive variable definitions`() {
		// given
		val projectProperties = ProjectProperties(parentVersion = "1.2", projectVersion = "2.10")
		projectProperties.add("module1.version", "\${module2.version}")
		projectProperties.add("module2.version", "\${module1.version}")

		// when
		val module1 = projectProperties.resolve("\${module1.version}")
		val module2 = projectProperties.resolve("\${module2.version}")
		// then
		assertThat(module1).isEqualTo("\${module1.version}")
		assertThat(module2).isEqualTo("\${module2.version}")
	}

	@Test
	fun `should resolve multiple variables in one value`() {
		// given
		val projectProperties = ProjectProperties(parentVersion = "1.2", projectVersion = "2.10")
		projectProperties.add("revision", "1.0.0")
		projectProperties.add("variant", "SNAPSHOT")

		// when
		val module = projectProperties.resolve("abc\${revision}-\${variant}def")

		// then
		assertThat(module).isEqualTo("abc1.0.0-SNAPSHOTdef")

	}


}