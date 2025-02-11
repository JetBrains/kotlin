/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.d8

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

@ExperimentalWasmDsl
@Deprecated(
    "Use 'org.jetbrains.kotlin.gradle.targets.wasm.d8.D8Plugin' instead",
    ReplaceWith("D8EnvSpec", "org.jetbrains.kotlin.gradle.targets.wasm.d8.D8Plugin")
)
open class D8Plugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply(org.jetbrains.kotlin.gradle.targets.wasm.d8.D8Plugin::class.java)

    }
}