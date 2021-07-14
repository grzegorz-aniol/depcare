package com.appga.depcaresuplier

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.neo4j.repository.config.EnableReactiveNeo4jRepositories
import org.springframework.jms.annotation.EnableJms

@SpringBootApplication
@EnableReactiveNeo4jRepositories
@EnableJms
class DepcareSuplierApplication

fun main(args: Array<String>) {
	runApplication<DepcareSuplierApplication>(*args)
}
