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
import kotlin.reflect.KMutableProperty1

fun mapKotlinTaskProperties(project: Project, task: AbstractKotlinCompile<*>) {
    val properties = PropertiesProvider(project)

    properties["kotlin.coroutines"]?.let {
        task.coroutinesFromGradleProperties = Coroutines.byCompilerArgument(it)
    }

    if (task is KotlinCompile) {
        properties["kotlin.incremental"]?.let {
            task.incremental = it.toBoolean()
        }
    }

    if (task is Kotlin2JsCompile) {
        properties["kotlin.incremental.js"]?.let {
            task.incremental = it.toBoolean()
        }
    }
}

private class PropertiesProvider(private val project: Project) {
    val localProperties = Properties()

    init {
        val localPropertiesFile = project.rootProject.file("local.properties")
        if (localPropertiesFile.isFile) {
            localPropertiesFile.inputStream().use {
                localProperties.load(it)
            }
        }
    }

    operator fun get(propName: String): String? =
            if (project.hasProperty(propName)) {
                project.property(propName) as? String
            }
            else {
                localProperties.getProperty(propName)
            }
}