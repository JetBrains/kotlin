/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.binaryen

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

/**
 * Specification for executing Binaryen, an optimization tool for wasm files.
 */
@Suppress("DEPRECATION")
@ExperimentalWasmDsl
abstract class BinaryenEnvSpec : org.jetbrains.kotlin.gradle.targets.js.binaryen.BinaryenEnvSpec() {

    companion object {
        const val EXTENSION_NAME: String = "kotlinBinaryenSpec"
    }
}