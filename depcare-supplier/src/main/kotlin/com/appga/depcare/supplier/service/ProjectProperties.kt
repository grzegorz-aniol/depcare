package com.appga.depcare.supplier.service

class ProjectProperties {
	private lateinit var projectVersion: String
	private var parentProjectVersion: String? = null
	private val properties = mutableMapOf<String, String>()
	private val regexVariable = Regex("\\$\\{([^\\}]+)\\}")
	private val projectVersionVariables = listOf("project.version", "version")
	private val parentVersionVariables = listOf("parent.project.version", "project.parent.version", "parent.version")

	constructor(parentVersion: String, projectVersion: String?) {
		setProjectVersion(parentVersion, projectVersion?.let { resolve(it) } ?: "")
	}

	fun add(key: String, value: String) {
		properties[key] = value
	}

	fun get(key: String): String? = properties[key]

	fun resolve(value: String?): String? {
		return value?.let { internalResolve(it, mutableSetOf()) }
	}

	fun setProjectVersion(parentVersion: String?, projectVersion: String) {
		this.parentProjectVersion = parentVersion
		this.projectVersion = projectVersion

		if (parentVersion?.isNotBlank() == true) {
			parentVersionVariables.forEach { variable -> add(variable, parentVersion) }
		}
		if (projectVersion.isNotBlank()) {
			projectVersionVariables.forEach { variable -> add(variable, projectVersion) }
		}
	}

	private fun internalResolve(value: String, variables: MutableSet<String>): String? {
		var input = value.trim()
		val replacements = regexVariable.findAll(input.trim()).map {
			val variableName = it.groups.get(1)?.value ?: ""
			val range = it.groups.get(0)?.range ?: IntRange(0, input.length - 1)
			if (variableName in variables) {
				return@map value to range
			}
			when {
				parentVersionVariables.any { it == variableName } -> parentProjectVersion
				projectVersionVariables.any { it == variableName } -> projectVersion
			}
			variables.add(variableName)
			val result = properties[variableName]?.let { internalResolve(it, variables) }
			variables.remove(variableName)

			result to range
		}.filter { it.first != null }
			.toList()
		var indexCorrection = 0
		replacements.forEach {
			val newValue = it.first ?: ""
			val range = it.second.let { IntRange(it.first + indexCorrection, it.endInclusive + indexCorrection) }
			val lengthBefore = input.length
			input = input.replaceRange(range, newValue)
			indexCorrection += input.length - lengthBefore
		}
		return input
	}
}