/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.dsl

import org.gradle.api.provider.ListProperty
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsSubTargetDsl

/**
 * [Wasmtime](https://wasmtime.dev) execution environment options for Kotlin WasmWasi targets.
 *
 * **Note:** This interface is not intended for implementation by build script or plugin authors.
 */
@ExperimentalWasmDsl
interface KotlinWasmtimeDsl : KotlinJsSubTargetDsl {

    /**
     * Specifies additional arguments to be passed to the Wasmtime runtime during execution.
     *
     * These arguments allow customization of the Wasmtime environment when executing
     * WebAssembly (Wasm) binaries. The property is typically used to configure runtime-specific
     * options such as enabling proposals, invoking specific functions, or adjusting behavior
     * for WebAssembly execution contexts.
     */
    val wasmtimeRunArgs: ListProperty<String>
}
