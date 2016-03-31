package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.tasks.compile.AbstractCompile
import java.util.*

fun mapKotlinTaskProperties(project: Project, task: AbstractCompile) {
    propertyMappings.forEach { it.apply(project, task) }

    val localPropertiesFile = project.rootProject.file("local.properties")
    if (localPropertiesFile.isFile) {
        val properties = Properties()
        properties.load(localPropertiesFile.inputStream())
        propertyMappings.forEach { it.apply(properties, task) }
    }
}

private val propertyMappings = listOf(
        KotlinPropertyMapping("kotlin.incremental", "incremental", String::toBoolean)
)

private class KotlinPropertyMapping<T>(
        private val projectPropName: String,
        private val taskPropName: String,
        private val transform: (String) -> T
) {
    fun apply(project: Project, task: AbstractCompile) {
        if (!project.hasProperty(projectPropName)) return

        setPropertyValue(task, project.property(projectPropName))
    }

    fun apply(properties: Properties, task: AbstractCompile) {
        setPropertyValue(task, properties.getProperty(projectPropName))
    }

    private fun setPropertyValue(task: AbstractCompile, value: Any?) {
        if (value !is String) return

        val transformedValue = transform(value) ?: return
        task.setProperty(taskPropName, transformedValue)
    }
}