package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.*
import kotlin.reflect.KMutableProperty1

fun mapKotlinTaskProperties(project: Project, task: KotlinCompile) {
    propertyMappings.forEach { it.apply(project, task) }

    val localPropertiesFile = project.rootProject.file("local.properties")
    if (localPropertiesFile.isFile) {
        val properties = Properties()
        properties.load(localPropertiesFile.inputStream())
        propertyMappings.forEach { it.apply(properties, task) }
    }
}

private val propertyMappings = listOf(
        KotlinPropertyMapping("kotlin.incremental", KotlinCompile::incremental, String::toBoolean)
)

private class KotlinPropertyMapping<T>(
        private val projectPropName: String,
        private val taskProperty: KMutableProperty1<KotlinCompile, T>,
        private val transform: (String) -> T
) {
    fun apply(project: Project, task: KotlinCompile) {
        if (!project.hasProperty(projectPropName)) return

        setPropertyValue(task, project.property(projectPropName))
    }

    fun apply(properties: Properties, task: KotlinCompile) {
        setPropertyValue(task, properties.getProperty(projectPropName))
    }

    private fun setPropertyValue(task: KotlinCompile, value: Any?) {
        if (value !is String) return

        val transformedValue = transform(value) ?: return
        taskProperty.set(task, transformedValue)
    }
}