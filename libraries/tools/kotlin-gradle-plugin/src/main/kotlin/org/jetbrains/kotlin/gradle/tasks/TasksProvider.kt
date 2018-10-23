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
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.defaultSourceSetName
import org.jetbrains.kotlin.gradle.plugin.sources.applyLanguageSettingsToKotlinTask

internal open class KotlinTasksProvider(val targetName: String) {
    open fun createKotlinJVMTask(
        project: Project,
        name: String,
        compilation: KotlinCompilation
    ): KotlinCompile =
        project.tasks.create(name, KotlinCompile::class.java).apply {
            configure(this, project, compilation)
        }

    fun createKotlinJSTask(project: Project, name: String, compilation: KotlinCompilation): Kotlin2JsCompile =
        project.tasks.create(name, Kotlin2JsCompile::class.java).apply {
            configure(this, project, compilation)
        }

    fun createKotlinCommonTask(project: Project, name: String, compilation: KotlinCompilation): KotlinCompileCommon =
        project.tasks.create(name, KotlinCompileCommon::class.java).apply {
            configure(this, project, compilation)
        }

    open fun configure(
        kotlinTask: AbstractKotlinCompile<*>,
        project: Project,
        compilation: KotlinCompilation
    ) {
        kotlinTask.sourceSetName = compilation.name
        kotlinTask.friendTaskName = taskToFriendTaskMapper[kotlinTask]
        mapKotlinTaskProperties(project, kotlinTask)

        project.whenEvaluated {
            val languageSettings = project.kotlinExtension.sourceSets.findByName(compilation.defaultSourceSetName)?.languageSettings
                ?: return@whenEvaluated

            kotlinTask as org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>
            applyLanguageSettingsToKotlinTask(languageSettings, kotlinTask)
        }
    }

    protected open val taskToFriendTaskMapper: TaskToFriendTaskMapper =
        RegexTaskToFriendTaskMapper.Default(targetName)
}

internal class AndroidTasksProvider(targetName: String) : KotlinTasksProvider(targetName) {
    override val taskToFriendTaskMapper: TaskToFriendTaskMapper =
        RegexTaskToFriendTaskMapper.Android(targetName)

    override fun configure(kotlinTask: AbstractKotlinCompile<*>, project: Project, compilation: KotlinCompilation) {
        super.configure(kotlinTask, project, compilation)
        kotlinTask.useModuleDetection = true
    }
}