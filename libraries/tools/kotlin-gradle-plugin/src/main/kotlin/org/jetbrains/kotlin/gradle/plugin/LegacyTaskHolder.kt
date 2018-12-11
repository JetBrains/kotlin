/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Task

class LegacyTaskHolder<T : Task>(private val task: T) : TaskHolder<T> {
    override fun doGetTask() = task

    override fun getTaskOrProvider(): Any = task

    override fun configure(action: (T) -> (Unit)) {
        with(task, action)
    }

    override fun toString(): String {
        return "TaskHolder instance: [className: ${javaClass.name}, task name: '${doGetTask().name}']"
    }
}