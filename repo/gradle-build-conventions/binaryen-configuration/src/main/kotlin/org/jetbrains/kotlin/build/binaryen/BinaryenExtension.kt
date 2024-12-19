/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalWasmDsl::class)
@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.build.binaryen

import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.Test
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.binaryen.BinaryenRootEnvSpec

abstract class BinaryenExtension(
    private val binaryenRoot: BinaryenRootEnvSpec,
    private val setupTask: TaskProvider<out Task>,
) {
    fun Test.setupBinaryen() {
        dependsOn(setupTask)
        val binaryenExecutablePath = binaryenRoot.executable
        doFirst {
            systemProperty("binaryen.path", binaryenExecutablePath.get())
        }
    }
}