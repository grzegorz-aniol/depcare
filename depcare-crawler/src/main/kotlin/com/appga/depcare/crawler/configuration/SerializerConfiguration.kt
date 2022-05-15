package com.appga.depcare.crawler.configuration

import com.appga.depcare.serialization.JsonSerializerFactory
import kotlinx.serialization.json.Json
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component

@Component
class SerializerConfiguration {

    @Bean
    fun jsonSerializer(): Json =
        JsonSerializerFactory.json

}
