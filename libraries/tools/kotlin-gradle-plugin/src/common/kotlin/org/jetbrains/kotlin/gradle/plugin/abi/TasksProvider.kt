/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.abi

import org.gradle.api.Task
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.tasks.locateTask

/**
 * Retrieves a task by its [name] and type [T], without triggering its creation or configuration.
 *
 * @throws IllegalArgumentException if a task with the name [name] isn't found
 */
internal inline fun <reified T : Task> TaskContainer.getTask(name: String): TaskProvider<T> =
    locateTask(name) ?: throw IllegalArgumentException("Couldn't locate task $name")
