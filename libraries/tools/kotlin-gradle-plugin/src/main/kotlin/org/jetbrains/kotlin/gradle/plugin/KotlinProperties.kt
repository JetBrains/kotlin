/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.*
import kotlin.reflect.KMutableProperty1

fun mapKotlinTaskProperties(project: Project, task: AbstractKotlinCompile<*>) {
    propertyMappings.forEach { it.apply(project, task) }

    val localPropertiesFile = project.rootProject.file("local.properties")
    if (localPropertiesFile.isFile) {
        val properties = Properties()
        properties.load(localPropertiesFile.inputStream())
        propertyMappings.forEach { it.apply(properties, task) }
    }
}

private val propertyMappings = listOf(
        KotlinPropertyMapping("kotlin.incremental", AbstractKotlinCompile<*>::incremental, String::toBoolean),
        KotlinPropertyMapping("kotlin.coroutines", AbstractKotlinCompile<*>::coroutinesFromGradleProperties) { Coroutines.byCompilerArgument(it) }
)

private class KotlinPropertyMapping<T>(
        private val projectPropName: String,
        private val taskProperty: KMutableProperty1<AbstractKotlinCompile<*>, T>,
        private val transform: (String) -> T
) {
    fun apply(project: Project, task: AbstractKotlinCompile<*>) {
        if (!project.hasProperty(projectPropName)) return

        setPropertyValue(task, project.property(projectPropName))
    }

    fun apply(properties: Properties, task: AbstractKotlinCompile<*>) {
        setPropertyValue(task, properties.getProperty(projectPropName))
    }

    private fun setPropertyValue(task: AbstractKotlinCompile<*>, value: Any?) {
        if (value !is String) return

        val transformedValue = transform(value) ?: return
        taskProperty.set(task, transformedValue)
    }
}