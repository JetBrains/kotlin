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
import org.gradle.api.tasks.TaskCollection
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrLink
import org.jetbrains.kotlin.gradle.tasks.configuration.*

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
    noinline body: ((T) -> (Unit))? = null
): TaskProvider<T> =
    this@registerTask.registerTask(name, T::class.java, args, body)

internal fun <T : Task> Project.registerTask(
    name: String,
    type: Class<T>,
    constructorArgs: List<Any> = emptyList(),
    body: ((T) -> (Unit))? = null
): TaskProvider<T> {
    val resultProvider = project.tasks.register(name, type, *constructorArgs.toTypedArray())
    if (body != null) {
        resultProvider.configure(body)
    }
    return resultProvider
}

internal fun TaskProvider<*>.dependsOn(other: TaskProvider<*>) = configure { it.dependsOn(other) }

internal fun TaskProvider<*>.dependsOn(other: Task) = configure { it.dependsOn(other) }

internal fun TaskProvider<*>.dependsOn(otherPath: String) = configure { it.dependsOn(otherPath) }

internal inline fun <reified S : Task> TaskCollection<in S>.withType(): TaskCollection<S> = withType(S::class.java)

/**
 * Locates a task by [name] and [type], without triggering its creation or configuration.
 */
internal inline fun <reified T : Task> Project.locateTask(name: String): TaskProvider<T>? = tasks.locateTask(name)

/**
 * Locates a task by [name] and [type], without triggering its creation or configuration.
 */
internal inline fun <reified T : Task> TaskContainer.locateTask(name: String): TaskProvider<T>? =
    if (names.contains(name)) named(name, T::class.java) else null

/**
 * Locates a task by [name] and [type], without triggering its creation or configuration or registers new task
 * with [name], type [T] and initialization script [body]
 */
internal inline fun <reified T : Task> Project.locateOrRegisterTask(name: String, noinline body: (T) -> (Unit)): TaskProvider<T> {
    return project.locateTask(name) ?: project.registerTask(name, T::class.java, body = body)
}

internal inline fun <reified T : Task> Project.locateOrRegisterTask(
    name: String,
    args: List<Any> = emptyList(),
    invokeWhenRegistered: (TaskProvider<T>.() -> Unit) = {},
    noinline configureTask: (T.() -> Unit)? = null
): TaskProvider<T> {
    locateTask<T>(name)?.let { return it }
    return registerTask(name, args, configureTask).also(invokeWhenRegistered)
}

internal open class KotlinTasksProvider {
    open fun registerKotlinJVMTask(
        project: Project,
        taskName: String,
        compilerOptions: KotlinJvmCompilerOptions,
        configuration: KotlinCompileConfig
    ): TaskProvider<out KotlinCompile> {
        return project.registerTask(taskName, KotlinCompile::class.java, constructorArgs = listOf(compilerOptions)).also {
            configuration.execute(it)
        }
    }

    fun registerKotlinJSTask(
        project: Project,
        taskName: String,
        compilerOptions: KotlinJsCompilerOptions,
        configuration: Kotlin2JsCompileConfig
    ): TaskProvider<out Kotlin2JsCompile> {
        return project.registerTask(
            taskName,
            Kotlin2JsCompile::class.java,
            constructorArgs = listOf(compilerOptions)
        ).also {
            configuration.execute(it)
        }
    }

    fun registerKotlinJsIrTask(
        project: Project, taskName: String, configuration: KotlinJsIrLinkConfig
    ): TaskProvider<out KotlinJsIrLink> {
        return project.registerTask(taskName, KotlinJsIrLink::class.java, listOf(project)).also {
            configuration.execute(it)
        }
    }

    fun registerKotlinCommonTask(
        project: Project,
        taskName: String,
        compilerOptions: KotlinMultiplatformCommonCompilerOptions,
        configuration: KotlinCompileCommonConfig
    ): TaskProvider<out KotlinCompileCommon> {
        return project.registerTask(
            taskName,
            KotlinCompileCommon::class.java,
            constructorArgs = listOf(compilerOptions)
        ).also {
            configuration.execute(it)
        }
    }
}
