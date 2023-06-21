/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl

import org.gradle.api.Action
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinWasmTargetDsl
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinWasmTargetPreset

@KotlinGradlePluginDsl
interface KotlinTargetContainerWithWasmPresetFunctions : KotlinTargetContainerWithPresetFunctions {
    @ExperimentalWasmDsl
    fun wasm(
        name: String = "wasm",
        configure: KotlinWasmTargetDsl.() -> Unit = { }
    ): KotlinJsTargetDsl =
        configureOrCreate(
            name,
            presets.getByName("wasm") as KotlinWasmTargetPreset,
            configure
        )

    @ExperimentalWasmDsl
    fun wasm() = wasm("wasm") { }

    @ExperimentalWasmDsl
    fun wasm(name: String) = wasm(name) { }

    @ExperimentalWasmDsl
    fun wasm(name: String, configure: Action<KotlinWasmTargetDsl>) = wasm(name) { configure.execute(this) }

    @ExperimentalWasmDsl
    fun wasm(configure: Action<KotlinWasmTargetDsl>) = wasm { configure.execute(this) }
}