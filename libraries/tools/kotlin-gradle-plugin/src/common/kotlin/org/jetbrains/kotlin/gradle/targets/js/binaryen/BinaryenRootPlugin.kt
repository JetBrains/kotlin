/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.binaryen

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

@Deprecated(
    "Use 'org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenRootPlugin' instead",
    ReplaceWith(
        "BinaryenRootPlugin",
        "org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenRootPlugin"
    )
)
@OptIn(ExperimentalWasmDsl::class)
open class BinaryenRootPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply(org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenRootPlugin::class.java)
    }

    companion object {
        const val TASKS_GROUP_NAME: String = org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenRootPlugin.TASKS_GROUP_NAME

        fun apply(rootProject: Project): org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenRootExtension =
            org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenRootPlugin.apply(rootProject)

        val Project.kotlinBinaryenExtension: org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenRootExtension
            get() = with(org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenRootPlugin.Companion) {
                this@kotlinBinaryenExtension.kotlinBinaryenExtension
            }
    }
}