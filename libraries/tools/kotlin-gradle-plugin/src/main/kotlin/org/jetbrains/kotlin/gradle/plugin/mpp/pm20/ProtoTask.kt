/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider

interface ProtoTask<T : Task> {
    fun registerTask(project: Project)

    fun nameIn(module: KotlinGradleModule): String
}

fun <T : Task> KotlinGradleModule.taskName(proto: ProtoTask<T>): String = proto.nameIn(this)

fun <T : Task> KotlinGradleModule.taskProvider(proto: ProtoTask<T>): TaskProvider<T> =
    project.tasks.named(taskName(proto)) as TaskProvider<T>
