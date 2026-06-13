/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.dsl

import org.gradle.api.Action
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.wasm.wasmtime.WasmtimeExec

/**
 * [Wasmtime](https://wasmtime.dev) execution environment options for Kotlin WasmWasi targets.
 *
 * **Note:** This interface is not intended for implementation by build script or plugin authors.
 */
@ExperimentalWasmDsl
interface KotlinWasmtimeDsl : KotlinJsSubTargetDsl {

    /**
     * Configure the default [WasmtimeExec] task that **runs** the Kotlin WasmWasi target.
     *
     * @see org.jetbrains.kotlin.gradle.targets.wasm.wasmtime.WasmtimeExec
     */
    fun runTask(body: WasmtimeExec.() -> Unit) {
        runTask(Action {
            body(it)
        })
    }

    /**
     * [Action] based version of [runTask] above.
     */
    fun runTask(body: Action<WasmtimeExec>)
}
