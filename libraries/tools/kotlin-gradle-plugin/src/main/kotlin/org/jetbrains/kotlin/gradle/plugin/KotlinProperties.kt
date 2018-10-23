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
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.*

fun mapKotlinTaskProperties(project: Project, task: AbstractKotlinCompile<*>) {
    PropertiesProvider(project).apply {
        coroutines?.let { task.coroutinesFromGradleProperties = it }
        useFallbackCompilerSearch?.let { task.useFallbackCompilerSearch = it }

        if (task is KotlinCompile) {
            incrementalJvm?.let { task.incremental = it }
            usePreciseJavaTracking?.let {
                task.usePreciseJavaTracking = it
            }
        }

        if (task is Kotlin2JsCompile) {
            incrementalJs?.let { task.incremental = it }
        }
    }
}

internal class PropertiesProvider(private val project: Project) {
    private val localProperties = Properties()

    init {
        val localPropertiesFile = project.rootProject.file("local.properties")
        if (localPropertiesFile.isFile) {
            localPropertiesFile.inputStream().use {
                localProperties.load(it)
            }
        }
    }

    val coroutines: Coroutines?
        get() = property("kotlin.coroutines")?.let { Coroutines.byCompilerArgument(it) }

    val incrementalJvm: Boolean?
        get() = booleanProperty("kotlin.incremental")

    val incrementalJs: Boolean?
        get() = booleanProperty("kotlin.incremental.js")

    val incrementalMultiplatform: Boolean?
        get() = booleanProperty("kotlin.incremental.multiplatform")

    val usePreciseJavaTracking: Boolean?
        get() = booleanProperty("kotlin.incremental.usePreciseJavaTracking")

    val useFallbackCompilerSearch: Boolean?
        get() = booleanProperty("kotlin.useFallbackCompilerSearch")

    private fun booleanProperty(propName: String): Boolean? =
        property(propName)?.toBoolean()

    private fun property(propName: String): String? =
        if (project.hasProperty(propName)) {
            project.property(propName) as? String
        } else {
            localProperties.getProperty(propName)
        }
}
