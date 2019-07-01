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
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.defaultSourceSetName
import org.jetbrains.kotlin.gradle.plugin.sources.applyLanguageSettingsToKotlinTask
import org.jetbrains.kotlin.gradle.utils.isGradleVersionAtLeast

internal val useLazyTaskConfiguration = org.jetbrains.kotlin.gradle.utils.isGradleVersionAtLeast(4, 9)
internal val canLocateTask = org.jetbrains.kotlin.gradle.utils.isGradleVersionAtLeast(5, 0)

/**
 * Registers the task with [name] and [type] and initialization script [body]
 * If Gradle with version <4.9 is used the task will be created
 */
@JvmName("registerTaskOld")
@Deprecated("please use Project.createOrRegisterTask", ReplaceWith("project.createOrRegisterTask(name, body)"))
internal fun <T : Task> registerTask(project: Project, name: String, type: Class<T>, body: (T) -> (Unit)): TaskHolder<T> =
    project.createOrRegisterTask(name, type, emptyList(), body)

internal inline fun <reified T : Task> Project.createOrRegisterTask(
    name: String,
    args: List<Any> = emptyList(),
    noinline body: (T) -> (Unit)
): TaskHolder<T> =
    createOrRegisterTask(name, T::class.java, args, body)

internal fun <T : Task> Project.createOrRegisterTask(
    name: String,
    type: Class<T>,
    constructorArgs: List<Any> = emptyList(), // note: args are only allowed with Gradle 4.7+
    body: (T) -> (Unit)
): TaskHolder<T> {
    return if (useLazyTaskConfiguration) {
        val provider = project.tasks.register(name, type, *constructorArgs.toTypedArray()).apply { configure(body) }
        TaskProviderHolder(name, project, provider)
    } else {
        val result = LegacyTaskHolder(if (constructorArgs.isEmpty()) {
            project.tasks.create(name, type)
        } else {
            if (!isGradleVersionAtLeast(4, 7)) {
                error("Cannot inject the arguments list into a task. This requires Gradle 4.7+.")
            }
            project.tasks.create(name, type, *constructorArgs.toTypedArray())
        })
        with(result.doGetTask(), body)
        result
    }
}


/**
 * Locates a task by [name] and [type], without triggering its creation or configuration.
 */
internal inline fun <reified T : Task> Project.locateTask(name: String): TaskHolder<T>? =
    if (canLocateTask) {
        try {
            TaskProviderHolder(name, this, tasks.named(name, T::class.java))
        } catch (e: UnknownTaskException) {
            null
        }
    } else {
        tasks.findByName(name)?.let {
            check(T::class.java.isInstance(it))

            @Suppress("UNCHECKED_CAST")
            LegacyTaskHolder(it as T)
        }
    }

/**
 * Locates a task by [name] and [type], without triggering its creation or configuration or registers new task
 * with [name], type [T] and initialization script [body]
 */
internal inline fun <reified T : Task> Project.locateOrRegisterTask(name: String, noinline body: (T) -> (Unit)): TaskHolder<T> {
    return project.locateTask(name) ?: registerTask(project, name, T::class.java, body)
}

internal open class KotlinTasksProvider(val targetName: String) {
    open fun registerKotlinJVMTask(
        project: Project,
        name: String,
        compilation: KotlinCompilation<*>,
        configureAction: (KotlinCompile) -> (Unit)
    ): TaskHolder<out KotlinCompile> {
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
        compilation: KotlinCompilation<*>,
        configureAction: (Kotlin2JsCompile) -> Unit
    ): TaskHolder<Kotlin2JsCompile> {
        val properties = PropertiesProvider(project)
        val taskClass = taskOrWorkersTask<Kotlin2JsCompile, Kotlin2JsCompileWithWorkers>(properties)
        val result = registerTask(project, name, taskClass) {
            configureAction(it)
        }
        configure(result, project, properties, compilation)
        return result
    }

    fun registerKotlinCommonTask(
        project: Project,
        name: String,
        compilation: KotlinCompilation<*>,
        configureAction: (KotlinCompileCommon) -> (Unit)
    ): TaskHolder<KotlinCompileCommon> {
        val properties = PropertiesProvider(project)
        val taskClass = taskOrWorkersTask<KotlinCompileCommon, KotlinCompileCommonWithWorkers>(properties)
        val result = registerTask(project, name, taskClass) {
            configureAction(it)
        }
        configure(result, project, properties, compilation)
        return result
    }

    open fun configure(
        kotlinTaskHolder: TaskHolder<AbstractKotlinCompile<*>>,
        project: Project,
        propertiesProvider: PropertiesProvider,
        compilation: KotlinCompilation<*>
    ) {
        val configureAfterEvaluated = RunOnceAfterEvaluated("TaskProvider.configure") {
            val languageSettings = project.kotlinExtension.sourceSets.findByName(compilation.defaultSourceSetName)?.languageSettings
                ?: return@RunOnceAfterEvaluated

            val kotlinTask = kotlinTaskHolder.doGetTask()
            kotlinTask as org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>
            applyLanguageSettingsToKotlinTask(languageSettings, kotlinTask)
        }
        kotlinTaskHolder.configure {
            it.sourceSetName = compilation.name
            it.friendTaskName = taskToFriendTaskMapper[it]
            propertiesProvider.mapKotlinTaskProperties(it)
            configureAfterEvaluated.onConfigure()
        }
        project.runOnceAfterEvaluated(configureAfterEvaluated, kotlinTaskHolder)
    }

    protected open val taskToFriendTaskMapper: TaskToFriendTaskMapper =
        RegexTaskToFriendTaskMapper.Default(targetName)

    private inline fun <reified Task, reified WorkersTask : Task> taskOrWorkersTask(properties: PropertiesProvider): Class<out Task> =
        if (properties.parallelTasksInProject != true) Task::class.java else WorkersTask::class.java
}

internal class AndroidTasksProvider(targetName: String) : KotlinTasksProvider(targetName) {
    override val taskToFriendTaskMapper: TaskToFriendTaskMapper =
        RegexTaskToFriendTaskMapper.Android(targetName)

    override fun configure(
        kotlinTaskHolder: TaskHolder<AbstractKotlinCompile<*>>,
        project: Project,
        propertiesProvider: PropertiesProvider,
        compilation: KotlinCompilation<*>
    ) {
        super.configure(kotlinTaskHolder, project, propertiesProvider, compilation)
        kotlinTaskHolder.configure {
            it.useModuleDetection = true
        }
    }
}