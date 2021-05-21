package org.appga.depcare.config

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.appga.depcare.domain.MvnGroupDir
import org.appga.depcare.domain.MvnLibraryDir
import org.appga.depcare.domain.MvnRepoDir
import org.appga.depcare.domain.MvnRootDir
import org.appga.depcare.domain.MvnVersionDir
import javax.inject.Singleton

@Singleton
class SerializerConfig {

    val json: Json

    init {
        val projectModule = SerializersModule {
            polymorphic(MvnRepoDir::class) {
                subclass(MvnRootDir::class, MvnRootDir.serializer())
                subclass(MvnGroupDir::class, MvnGroupDir.serializer())
                subclass(MvnLibraryDir::class, MvnLibraryDir.serializer())
                subclass(MvnVersionDir::class, MvnVersionDir.serializer())
            }
        }
        json = Json { serializersModule = projectModule }
    }
}
