package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.tasks.compile.AbstractCompile

fun mapKotlinTaskProperties(project: Project, task: AbstractCompile) {
    for (mapping in propertyMappings) {
        mapping.apply(project, task)
    }
}

private val propertyMappings = listOf(
        KotlinPropertyMapping("kotlin.incremental", "experimentalIncremental", String::toBoolean)
)

private class KotlinPropertyMapping<T>(
        private val projectPropName: String,
        private val taskPropName: String,
        private val transform: (String) -> T
) {
    fun apply(project: Project, task: AbstractCompile) {
        if (!project.hasProperty(projectPropName)) return

        val value = project.property(projectPropName) as? String ?: return
        val transformedValue = transform(value) ?: return
        task.setProperty(taskPropName, transformedValue)
    }
}