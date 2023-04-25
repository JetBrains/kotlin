/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider

const val KOTLIN_BUILD_DIR_NAME = "kotlin"
const val USING_JVM_INCREMENTAL_COMPILATION_MESSAGE = "Using Kotlin/JVM incremental compilation"
const val USING_JS_INCREMENTAL_COMPILATION_MESSAGE = "Using Kotlin/JS incremental compilation"
const val USING_JS_IR_BACKEND_MESSAGE = "Using Kotlin/JS IR backend"

internal inline val <reified T : Task> T.thisTaskProvider: TaskProvider<out T>
    get() = checkNotNull(project.locateTask<T>(name))