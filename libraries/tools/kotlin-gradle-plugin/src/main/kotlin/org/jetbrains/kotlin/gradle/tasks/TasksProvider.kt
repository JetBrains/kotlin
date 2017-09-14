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
import org.jetbrains.kotlin.gradle.plugin.RegexTaskToFriendTaskMapper
import org.jetbrains.kotlin.gradle.plugin.TaskToFriendTaskMapper
import org.jetbrains.kotlin.gradle.plugin.mapKotlinTaskProperties

internal open class KotlinTasksProvider {
    fun createKotlinJVMTask(project: Project, name: String, sourceSetName: String): KotlinCompile =
            project.tasks.create(name, KotlinCompile::class.java).apply {
                configure(project, sourceSetName)
            }

    fun createKotlinJSTask(project: Project, name: String, sourceSetName: String): Kotlin2JsCompile =
            project.tasks.create(name, Kotlin2JsCompile::class.java).apply {
                configure(project, sourceSetName)
            }

    fun createKotlinCommonTask(project: Project, name: String, sourceSetName: String): KotlinCompileCommon =
            project.tasks.create(name, KotlinCompileCommon::class.java).apply {
                configure(project, sourceSetName)
            }

    private fun AbstractKotlinCompile<*>.configure(project: Project, sourceSetName: String) {
        this.sourceSetName = sourceSetName
        this.friendTaskName = taskToFriendTaskMapper[this]
        mapKotlinTaskProperties(project, this)
        outputs.upToDateWhen { isCacheFormatUpToDate }
    }

    protected open val taskToFriendTaskMapper: TaskToFriendTaskMapper =
            RegexTaskToFriendTaskMapper.Default()
}

internal class KotlinCommonTasksProvider : KotlinTasksProvider() {
    override val taskToFriendTaskMapper: TaskToFriendTaskMapper =
            RegexTaskToFriendTaskMapper.Common()
}

internal class Kotlin2JsTasksProvider : KotlinTasksProvider() {
    override val taskToFriendTaskMapper: TaskToFriendTaskMapper =
            RegexTaskToFriendTaskMapper.JavaScript()
}

internal class AndroidTasksProvider : KotlinTasksProvider() {
    override val taskToFriendTaskMapper: TaskToFriendTaskMapper =
            RegexTaskToFriendTaskMapper.Android()
}