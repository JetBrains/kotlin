/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.tasks.dependsOn
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.tooling.core.UnsafeApi

@Idea222Api
internal fun Project.ideaImportDependsOn(task: TaskProvider<*>) {
    @OptIn(UnsafeApi::class)
    prepareKotlinIdeaImportTask.dependsOn(task)
}

@UnsafeApi("Use 'ideaImportDependsOn' instead")
internal val Project.prepareKotlinIdeaImportTask: TaskProvider<Task>
    get() = locateOrRegisterTask(
        "prepareKotlinIdeaImport",
        configureTask = {
            description = "Umbrella for all tasks required to run before IDEA/Gradle import"
        }
    )
