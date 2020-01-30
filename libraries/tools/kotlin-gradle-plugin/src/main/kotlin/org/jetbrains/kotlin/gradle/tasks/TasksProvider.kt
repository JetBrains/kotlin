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

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.UnknownTaskException
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.mapKotlinTaskProperties
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractKotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.runOnceAfterEvaluated
import org.jetbrains.kotlin.gradle.plugin.sources.applyLanguageSettingsToKotlinOptions
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrLink
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrLinkWithWorkers

/**
 * Registers the task with [name] and [type] and initialization script [body]
 */
@JvmName("registerTaskOld")
@Deprecated("please use Project.registerTask", ReplaceWith("project.registerTask(name, type, emptyList(), body)"))
internal fun <T : Task> registerTask(project: Project, name: String, type: Class<T>, body: (T) -> (Unit)): TaskProvider<T> =
    project.registerTask(name, type, emptyList(), body)

internal inline fun <reified T : Task> Project.registerTask(
    name: String,
    args: List<Any> = emptyList(),
    noinline body: (T) -> (Unit)
): TaskProvider<T> =
    this@registerTask.registerTask(name, T::class.java, args, body)

internal fun <T : Task> Project.registerTask(
    name: String,
    type: Class<T>,
    constructorArgs: List<Any> = emptyList(),
    body: (T) -> (Unit)
): TaskProvider<T> {
    return project.tasks.register(name, type, *constructorArgs.toTypedArray()).apply { configure(body) }
}


/**
 * Locates a task by [name] and [type], without triggering its creation or configuration.
 */
internal inline fun <reified T : Task> Project.locateTask(name: String): TaskProvider<T>? =
    try {
        tasks.withType(T::class.java).named(name)
    } catch (e: UnknownTaskException) {
        null
    }

/**
 * Locates a task by [name] and [type], without triggering its creation or configuration or registers new task
 * with [name], type [T] and initialization script [body]
 */
internal inline fun <reified T : Task> Project.locateOrRegisterTask(name: String, noinline body: (T) -> (Unit)): TaskProvider<T> {
    return project.locateTask(name) ?: registerTask(project, name, T::class.java, body)
}

internal open class KotlinTasksProvider(val targetName: String) {
    open fun registerKotlinJVMTask(
        project: Project,
        name: String,
        compilation: AbstractKotlinCompilation<*>,
        configureAction: (KotlinCompile) -> (Unit)
    ): TaskProvider<out KotlinCompile> {
        val properties = PropertiesProvider(project)
        val taskClass = taskOrWorkersTask<KotlinCompile, KotlinCompileWithWorkers>(properties)
        val result = registerTask(project, name, taskClass) {
            configureAction(it)
        }
        configure(result, project, properties, compilation)
        return result
    }

    fun registerKotlinJSTask(
        project: Project,
        name: String,
        compilation: AbstractKotlinCompilation<*>,
        configureAction: (Kotlin2JsCompile) -> Unit
    ): TaskProvider<out Kotlin2JsCompile> {
        val properties = PropertiesProvider(project)
        val taskClass = taskOrWorkersTask<Kotlin2JsCompile, Kotlin2JsCompileWithWorkers>(properties)
        val result = project.registerTask(name, taskClass) {
            configureAction(it)
        }
        configure(result, project, properties, compilation)
        return result
    }

    fun registerKotlinJsIrTask(
        project: Project,
        name: String,
        compilation: AbstractKotlinCompilation<*>,
        configureAction: (KotlinJsIrLink) -> Unit
    ): TaskProvider<out KotlinJsIrLink> {
        val properties = PropertiesProvider(project)
        val taskClass = taskOrWorkersTask<KotlinJsIrLink, KotlinJsIrLinkWithWorkers>(properties)
        val result = project.registerTask(name, taskClass) {
            configureAction(it)
        }
        configure(result, project, properties, compilation)
        return result
    }

    fun registerKotlinCommonTask(
        project: Project,
        name: String,
        compilation: AbstractKotlinCompilation<*>,
        configureAction: (KotlinCompileCommon) -> (Unit)
    ): TaskProvider<out KotlinCompileCommon> {
        val properties = PropertiesProvider(project)
        val taskClass = taskOrWorkersTask<KotlinCompileCommon, KotlinCompileCommonWithWorkers>(properties)
        val result = project.registerTask(name, taskClass) {
            configureAction(it)
        }
        configure(result, project, properties, compilation)
        return result
    }

    open fun configure(
        kotlinTaskHolder: TaskProvider<out AbstractKotlinCompile<*>>,
        project: Project,
        propertiesProvider: PropertiesProvider,
        compilation: AbstractKotlinCompilation<*>
    ) {
        project.runOnceAfterEvaluated("apply properties and language settings to ${kotlinTaskHolder.name}", kotlinTaskHolder) {
            propertiesProvider.mapKotlinTaskProperties(kotlinTaskHolder.get())

            applyLanguageSettingsToKotlinOptions(
                compilation.defaultSourceSet.languageSettings,
                (kotlinTaskHolder.get() as org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>).kotlinOptions
            )
        }
    }

    private inline fun <reified Task, reified WorkersTask : Task> taskOrWorkersTask(properties: PropertiesProvider): Class<out Task> =
        if (properties.parallelTasksInProject != true) Task::class.java else WorkersTask::class.java
}

internal class AndroidTasksProvider(targetName: String) : KotlinTasksProvider(targetName) {
    override fun configure(
        kotlinTaskHolder: TaskProvider<out AbstractKotlinCompile<*>>,
        project: Project,
        propertiesProvider: PropertiesProvider,
        compilation: AbstractKotlinCompilation<*>
    ) {
        super.configure(kotlinTaskHolder, project, propertiesProvider, compilation)
        kotlinTaskHolder.configure {
            it.useModuleDetection = true
        }
    }
}