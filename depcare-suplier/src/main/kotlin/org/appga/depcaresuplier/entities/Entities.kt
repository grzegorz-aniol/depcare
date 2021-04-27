package org.appga.depcaresuplier.entities

import org.springframework.data.neo4j.core.schema.Id
import org.springframework.data.neo4j.core.schema.Node
import org.springframework.data.neo4j.core.schema.Property
import org.springframework.data.neo4j.core.schema.Relationship

@Node("Library")
data class Library (
	@field:Id
	var id: String,

	@field:Property("name")
	var name: String,

	@field:Property("location")
	var location: String,

	@field:Property("package")
	var packageName: String,

	@field:Relationship(type = "VERSION_OF", direction = Relationship.Direction.INCOMING)
	var libraryVersions: MutableSet<LibraryVersion> = mutableSetOf()
)

@Node("LibraryVersion")
data class LibraryVersion (
	@field:Id
	var id: String,

	@field:Property("name")
	var name: String,

	@field:Property("version")
	var version: String,

	@field:Property("location")
	var location: String,
)

