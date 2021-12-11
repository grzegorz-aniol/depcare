package com.appga.depcare.crawler

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.neo4j.repository.config.EnableReactiveNeo4jRepositories

@SpringBootApplication
@EnableReactiveNeo4jRepositories
class DepcareCrawlerApplication

fun main(args: Array<String>) {
	runApplication<DepcareCrawlerApplication>(*args)
}
