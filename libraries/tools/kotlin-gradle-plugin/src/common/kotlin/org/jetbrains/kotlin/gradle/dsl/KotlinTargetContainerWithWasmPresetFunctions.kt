/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl

import groovy.lang.Closure
import org.gradle.util.ConfigureUtil
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinWasmTargetDsl
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTargetPreset
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinWasmTargetPreset

interface KotlinTargetContainerWithWasmPresetFunctions : KotlinTargetContainerWithPresetFunctions {
    fun wasm(
        name: String = "wasm",
        configure: KotlinWasmTargetDsl.() -> Unit = { }
    ): KotlinJsTargetDsl =
        configureOrCreate(
            name,
            presets.getByName("wasm") as KotlinWasmTargetPreset,
            configure
        )

    fun wasm() = wasm("wasm") { }
    fun wasm(name: String) = wasm(name) { }
    fun wasm(name: String, configure: Closure<*>) = wasm(name) { ConfigureUtil.configure(configure, this) }
    fun wasm(configure: Closure<*>) = wasm { ConfigureUtil.configure(configure, this) }
}