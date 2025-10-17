/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.binaryen

import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenEnvSpec

@OptIn(ExperimentalWasmDsl::class)
abstract class BinaryenExtension(
    private val binaryen: BinaryenEnvSpec,
) {
    val Project.binaryenVersion: String get() = property("versions.binaryen") as String

    fun Test.setupBinaryen() {
        with(binaryen) {
            dependsOn(project.binaryenSetupTaskProvider)
        }

        val binaryenExecutablePath = binaryen.executable
        doFirst {
            systemProperty("binaryen.path", binaryenExecutablePath.get())
        }
    }
}