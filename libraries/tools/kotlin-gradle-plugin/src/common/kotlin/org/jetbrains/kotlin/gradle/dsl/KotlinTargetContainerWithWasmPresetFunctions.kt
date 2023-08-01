/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl

import org.gradle.api.Action
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinWasmJsTargetDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinWasmWasiTargetDsl
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinWasmTargetPreset

@KotlinGradlePluginDsl
interface KotlinTargetContainerWithWasmPresetFunctions : KotlinTargetContainerWithPresetFunctions {
    @ExperimentalWasmDsl
    fun wasmJs(
        name: String = "wasmJs",
        configure: KotlinWasmJsTargetDsl.() -> Unit = { },
    ): KotlinWasmJsTargetDsl =
        configureOrCreate(
            name,
            @Suppress("DEPRECATION")
            presets.getByName("wasmJs") as KotlinWasmTargetPreset,
            configure
        )

    @ExperimentalWasmDsl
    fun wasmJs() = wasmJs("wasmJs") { }

    @ExperimentalWasmDsl
    fun wasmJs(name: String) = wasmJs(name) { }

    @ExperimentalWasmDsl
    fun wasmJs(name: String, configure: Action<KotlinWasmJsTargetDsl>) = wasmJs(name) { configure.execute(this) }

    @ExperimentalWasmDsl
    fun wasmJs(configure: Action<KotlinWasmJsTargetDsl>) = wasmJs { configure.execute(this) }

    @ExperimentalWasmDsl
    fun wasmWasi(
        name: String = "wasmWasi",
        configure: KotlinWasmWasiTargetDsl.() -> Unit = { },
    ): KotlinWasmWasiTargetDsl =
        configureOrCreate(
            name,
            @Suppress("DEPRECATION")
            presets.getByName("wasmWasi") as KotlinWasmTargetPreset,
            configure
        )

    @ExperimentalWasmDsl
    fun wasmWasi() = wasmJs("wasmWasi") { }

    @ExperimentalWasmDsl
    fun wasmWasi(name: String) = wasmWasi(name) { }

    @ExperimentalWasmDsl
    fun wasmWasi(name: String, configure: Action<KotlinWasmWasiTargetDsl>) = wasmWasi(name) { configure.execute(this) }

    @ExperimentalWasmDsl
    fun wasmWasi(configure: Action<KotlinWasmWasiTargetDsl>) = wasmWasi { configure.execute(this) }

    @Deprecated("Use wasmJs instead", replaceWith = ReplaceWith("wasmJs(name, configure)"))
    @ExperimentalWasmDsl
    fun wasm(
        name: String = "wasmJs",
        configure: KotlinWasmJsTargetDsl.() -> Unit = { },
    ): KotlinWasmJsTargetDsl = wasmJs(name, configure)

    @Deprecated("Use wasmJs instead", replaceWith = ReplaceWith("wasmJs()"))
    @ExperimentalWasmDsl
    fun wasm() = wasmJs()

    @Deprecated("Use wasmJs instead", replaceWith = ReplaceWith("wasmJs(name)"))
    @ExperimentalWasmDsl
    fun wasm(name: String) = wasmJs(name)

    @Deprecated("Use wasmJs instead", replaceWith = ReplaceWith("wasmJs(name, configure)"))
    @ExperimentalWasmDsl
    fun wasm(name: String, configure: Action<KotlinWasmJsTargetDsl>) = wasmJs(name, configure)

    @Deprecated("Use wasmJs instead", replaceWith = ReplaceWith("wasmJs(configure)"))
    @ExperimentalWasmDsl
    fun wasm(configure: Action<KotlinWasmJsTargetDsl>) = wasmJs(configure)
}