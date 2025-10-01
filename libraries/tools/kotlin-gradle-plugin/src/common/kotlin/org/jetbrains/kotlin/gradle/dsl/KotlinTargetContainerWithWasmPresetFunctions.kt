/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl

import org.gradle.api.Action
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinWasmJsTargetDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinWasmSpecTargetDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinWasmWasiTargetDsl

@KotlinGradlePluginPublicDsl
interface KotlinTargetContainerWithWasmPresetFunctions : KotlinTargetContainerWithPresetFunctions {
    @ExperimentalWasmDsl
    fun wasmJs(
        name: String = DEFAULT_WASM_JS_NAME,
        configure: KotlinWasmJsTargetDsl.() -> Unit = { },
    ): KotlinWasmJsTargetDsl

    @ExperimentalWasmDsl
    fun wasmJs() = wasmJs { }

    @ExperimentalWasmDsl
    fun wasmJs(name: String) = wasmJs(name) { }

    @ExperimentalWasmDsl
    fun wasmJs(name: String, configure: Action<KotlinWasmJsTargetDsl>) = wasmJs(name) { configure.execute(this) }

    @ExperimentalWasmDsl
    fun wasmJs(configure: Action<KotlinWasmJsTargetDsl>) = wasmJs { configure.execute(this) }

    @ExperimentalWasmDsl
    fun wasmWasi(
        name: String = DEFAULT_WASM_WASI_NAME,
        configure: KotlinWasmWasiTargetDsl.() -> Unit = { },
    ): KotlinWasmWasiTargetDsl

    @ExperimentalWasmDsl
    fun wasmWasi() = wasmWasi { }

    @ExperimentalWasmDsl
    fun wasmWasi(name: String) = wasmWasi(name) { }

    @ExperimentalWasmDsl
    fun wasmWasi(name: String, configure: Action<KotlinWasmWasiTargetDsl>) = wasmWasi(name) { configure.execute(this) }

    @ExperimentalWasmDsl
    fun wasmWasi(configure: Action<KotlinWasmWasiTargetDsl>) = wasmWasi { configure.execute(this) }

    @ExperimentalWasmDsl
    fun wasmSpec(
        name: String = DEFAULT_WASM_SPEC_NAME,
        configure: KotlinWasmSpecTargetDsl.() -> Unit = { }
    ): KotlinWasmSpecTargetDsl

    @ExperimentalWasmDsl
    fun wasmSpec() = wasmSpec { }

    @ExperimentalWasmDsl
    fun wasmSpec(name: String) = wasmSpec(name) { }

    @ExperimentalWasmDsl
    fun wasmSpec(name: String, configure: Action<KotlinWasmSpecTargetDsl>) = wasmSpec(name) { configure.execute(this) }

    @ExperimentalWasmDsl
    fun wasmSpec(configure: Action<KotlinWasmSpecTargetDsl>) = wasmSpec { configure.execute(this) }

    @Deprecated(
        "Use wasmJs instead. Scheduled for removal in Kotlin 2.3.",
        replaceWith = ReplaceWith("wasmJs(name, configure)"),
        level = DeprecationLevel.ERROR
    )
    @ExperimentalWasmDsl
    fun wasm(
        name: String = DEFAULT_WASM_JS_NAME,
        configure: KotlinWasmJsTargetDsl.() -> Unit = { },
    ): KotlinWasmJsTargetDsl = wasmJs(name, configure)

    @Deprecated(
        "Use wasmJs instead. Scheduled for removal in Kotlin 2.3.",
        replaceWith = ReplaceWith("wasmJs()"),
        level = DeprecationLevel.ERROR
    )
    @ExperimentalWasmDsl
    fun wasm() = wasmJs()

    @Deprecated(
        "Use wasmJs instead. Scheduled for removal in Kotlin 2.3.",
        replaceWith = ReplaceWith("wasmJs(name)"),
        level = DeprecationLevel.ERROR
    )
    @ExperimentalWasmDsl
    fun wasm(name: String) = wasmJs(name)

    @Deprecated(
        "Use wasmJs instead. Scheduled for removal in Kotlin 2.3.",
        replaceWith = ReplaceWith("wasmJs(name, configure)"),
        level = DeprecationLevel.ERROR
    )
    @ExperimentalWasmDsl
    fun wasm(name: String, configure: Action<KotlinWasmJsTargetDsl>) = wasmJs(name, configure)

    @Deprecated(
        "Use wasmJs instead. Scheduled for removal in Kotlin 2.3.",
        replaceWith = ReplaceWith("wasmJs(configure)"),
        level = DeprecationLevel.ERROR
    )
    @ExperimentalWasmDsl
    fun wasm(configure: Action<KotlinWasmJsTargetDsl>) = wasmJs(configure)

    @InternalKotlinGradlePluginApi
    companion object {
        internal const val DEFAULT_WASM_JS_NAME = "wasmJs"
        internal const val DEFAULT_WASM_WASI_NAME = "wasmWasi"
        internal const val DEFAULT_WASM_SPEC_NAME = "wasmSpec"
    }
}