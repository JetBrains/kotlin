/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.d8

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

/**
 * Spec for D8 - this target is available only for Wasm
 */
@Suppress("DEPRECATION")
@ExperimentalWasmDsl
abstract class D8EnvSpec internal constructor() : org.jetbrains.kotlin.gradle.targets.js.d8.D8EnvSpec() {

    companion object {
        const val EXTENSION_NAME: String = "kotlinD8Spec"
    }
}