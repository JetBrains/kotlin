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
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.tasks.TaskProvider
import org.gradle.language.base.plugins.LifecycleBasePlugin

internal inline fun <reified T : Any> Any.addExtension(name: String, extension: T) =
    (this as ExtensionAware).extensions.add(T::class.java, name, extension)

internal inline fun <reified T : Any> Any.getExtension(name: String): T? =
    (this as ExtensionAware).extensions.getByName(name) as T?

internal inline fun <reified T : Any> Any.findExtension(name: String): T? =
    (this as ExtensionAware).extensions.findByName(name)?.let { it as T? }

inline val ExtensionAware.extraProperties: ExtraPropertiesExtension
    get() = extensions.extraProperties

@JvmName("getOrNullTyped")
internal inline fun <reified T : Any> ExtraPropertiesExtension.getOrNull(name: String): T? {
    return if (has(name)) get(name) as T else null
}

internal fun ExtraPropertiesExtension.getOrNull(name: String): Any? {
    return if (has(name)) get(name) else null
}

/**
 * Configures the project's 'assemble' task to depend on the provided [task].
 *
 * This ensures that when `./gradlew assemble` is run, the provided [task]
 * will execute as part of the build lifecycle.
 *
 * @param task The provider of the task to be added to the assemble lifecycle.
 */
internal fun Project.addToAssemble(task: TaskProvider<*>) {
    tasks.named(LifecycleBasePlugin.ASSEMBLE_TASK_NAME).configure { assemble ->
        assemble.dependsOn(task)
    }
}