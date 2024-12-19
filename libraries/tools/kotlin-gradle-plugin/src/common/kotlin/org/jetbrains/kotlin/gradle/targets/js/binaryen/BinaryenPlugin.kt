/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.binaryen

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

@Deprecated(
    "Use 'org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenPlugin' instead",
    ReplaceWith(
        "BinaryenPlugin",
        "org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenPlugin"
    )
)
@OptIn(ExperimentalWasmDsl::class)
abstract class BinaryenPlugin internal constructor() : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply(org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenPlugin::class.java)
    }

    companion object {
        const val TASKS_GROUP_NAME: String = org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenPlugin.TASKS_GROUP_NAME

        fun apply(rootProject: Project): org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenExtension =
            org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenPlugin.apply(rootProject)

        val Project.kotlinBinaryenExtension: org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenExtension
            get() = with(org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenPlugin.Companion) {
                this@kotlinBinaryenExtension.kotlinBinaryenExtension
            }
    }
}
