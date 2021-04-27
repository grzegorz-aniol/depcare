package org.appga.depcaresuplier.repositories

import org.appga.depcaresuplier.entities.Library
import org.appga.depcaresuplier.entities.LibraryVersion
import org.neo4j.driver.internal.shaded.reactor.core.publisher.Mono
import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository

interface LibraryRepository : ReactiveNeo4jRepository<Library, String> {
	fun findOneByName(name: String): Mono<Library>
}

interface LibraryVersionRepository : ReactiveNeo4jRepository<LibraryVersion, String> {
	fun findOneByNameAndVersion(name: String, version: String): Mono<LibraryVersion>
}