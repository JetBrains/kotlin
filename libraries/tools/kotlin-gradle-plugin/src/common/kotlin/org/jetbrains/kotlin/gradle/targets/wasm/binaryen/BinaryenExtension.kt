/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.binaryen

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

@Suppress("DEPRECATION")
@OptIn(ExperimentalWasmDsl::class)
open class BinaryenExtension(
    rootProject: Project,
    binaryenSpec: BinaryenEnvSpec,
) : org.jetbrains.kotlin.gradle.targets.js.binaryen.BinaryenExtension(
    rootProject,
    binaryenSpec
) {
    companion object {
        const val EXTENSION_NAME: String = "kotlinBinaryen"
    }
}