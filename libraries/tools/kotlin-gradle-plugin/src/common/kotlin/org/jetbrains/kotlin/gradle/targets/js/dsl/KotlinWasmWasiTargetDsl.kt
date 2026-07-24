/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.dsl

import org.gradle.api.Action
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.plugin.mpp.HasBinaries
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsBinaryContainer
import org.jetbrains.kotlin.gradle.targets.wasm.dsl.KotlinWasmtimeDsl

/**
 * Base configuration options for the compilation of Kotlin WasmWasi targets.
 *
 * ```
 * kotlin {
 *     wasmWasi { // Creates WasmWasi target
 *         // Configure WasmWasi target specifics here
 *     }
 * }
 * ```
 *
 * To learn more see:
 * - [Get started with Kotlin/Wasm and WASI](https://kotl.in/kotlin-wasm-wasi-setup).
 *
 * **Note:** This interface is not intended for implementation by build script or plugin authors.
 */
interface KotlinWasmWasiTargetDsl :
    KotlinWasmTargetDsl,
    KotlinTargetWithNodeJsDsl,
    HasBinaries<KotlinJsBinaryContainer> {

    /**
     * Enable [Wasmtime](https://wasmtime.dev) as the execution environment for this target.
     *
     * When enabled, Kotlin Gradle plugin will download and install
     * the required environment and dependencies for running and testing
     * using Wasmtime.
     *
     * @see org.jetbrains.kotlin.gradle.targets.wasm.dsl.KotlinWasmtimeDsl
     */
    @ExperimentalWasmDsl
    fun wasmtime() = wasmtime { }

    /**
     * Enable [Wasmtime](https://wasmtime.dev) as the execution environment for this target.
     *
     * When enabled, Kotlin Gradle plugin will download and install
     * the required environment and dependencies for running and testing
     * using Wasmtime.
     *
     * The target can be configured using [body].
     *
     * @see org.jetbrains.kotlin.gradle.targets.wasm.dsl.KotlinWasmtimeDsl
     */
    @ExperimentalWasmDsl
    fun wasmtime(body: KotlinWasmtimeDsl.() -> Unit)

    /**
     * [Action] based version of [wasmtime] above.
     */
    @ExperimentalWasmDsl
    fun wasmtime(fn: Action<KotlinWasmtimeDsl>) {
        wasmtime {
            fn.execute(this)
        }
    }
}
