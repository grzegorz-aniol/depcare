package com.appga.depcaresuplier

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import com.appga.depcare.domain.MvnLibraryDir
import com.appga.depcare.domain.MvnGroupDir
import com.appga.depcare.domain.MvnRepoDir
import com.appga.depcare.domain.MvnRootDir
import com.appga.depcare.domain.MvnVersionDir
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