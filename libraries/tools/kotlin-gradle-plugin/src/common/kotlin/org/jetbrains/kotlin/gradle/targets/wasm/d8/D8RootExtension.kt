/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.d8

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

@OptIn(ExperimentalWasmDsl::class)
open class D8RootExtension(
    project: Project,
    d8EnvSpec: D8EnvSpec,
) : org.jetbrains.kotlin.gradle.targets.js.d8.D8RootExtension(project, d8EnvSpec) {

    companion object {
        const val EXTENSION_NAME: String = "kotlinD8"
    }
}