/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.Task

class LegacyTaskHolder<out T : Task>(private val task: T) : TaskHolder<T> {
    override val project: Project
        get() = task.project

    override val name: String
        get() = task.name

    override fun doGetTask() = task

    override fun getTaskOrProvider(): Any = task

    override fun configure(action: (T) -> (Unit)) {
        with(task, action)
    }

    override fun toString(): String {
        return "TaskHolder instance: [className: ${javaClass.name}, task name: '${doGetTask().name}']"
    }
}