/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.Buildable
import org.gradle.api.Task
import org.gradle.api.tasks.TaskDependency

internal class AggregatedTaskDependencies(
    private vararg val taskDependencies: TaskDependency
) : TaskDependency {
    constructor(vararg buildables: Buildable) : this(*(buildables.map { it.buildDependencies }.toTypedArray()))

    override fun getDependencies(task: Task?): Set<Task> = taskDependencies.flatMap { it.getDependencies(task) }.toSet()
}