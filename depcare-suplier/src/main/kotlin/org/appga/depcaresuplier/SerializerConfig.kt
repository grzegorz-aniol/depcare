package org.appga.depcaresuplier

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.appga.depcare.domain.MvnLibraryDir
import org.appga.depcare.domain.MvnGroupDir
import org.appga.depcare.domain.MvnRepoDir
import org.appga.depcare.domain.MvnRootDir
import org.appga.depcare.domain.MvnVersionDir
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SerializerConfig {

	@Bean
	fun kserializerModule(): Json {
		val projectModule = SerializersModule {
			polymorphic(MvnRepoDir::class) {
				subclass(MvnRootDir::class, MvnRootDir.serializer())
				subclass(MvnGroupDir::class, MvnGroupDir.serializer())
				subclass(MvnLibraryDir::class, MvnLibraryDir.serializer())
				subclass(MvnVersionDir::class, MvnVersionDir.serializer())
			}
		}
		val json = Json { serializersModule = projectModule }
		return json
	}
}