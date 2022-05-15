package com.appga.depcare.serialization

import com.appga.depcare.domain.MvnGroupDir
import com.appga.depcare.domain.MvnLibraryDir
import com.appga.depcare.domain.MvnRepoDir
import com.appga.depcare.domain.MvnRootDir
import com.appga.depcare.domain.MvnVersionDir
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

object JsonSerializerFactory {
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