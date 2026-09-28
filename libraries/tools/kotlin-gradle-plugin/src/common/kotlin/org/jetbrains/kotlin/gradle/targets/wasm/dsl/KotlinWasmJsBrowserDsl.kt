/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.dsl

import org.gradle.api.Action
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBrowserDsl
import org.jetbrains.kotlin.gradle.tasks.KotlinWasmDevServer

/**
 * DSL for configuring browser-specific settings for Kotlin Wasm JS target.
 *
 * This interface extends the [KotlinJsBrowserDsl] for additional configuration
 * tailored to Kotlin Wasm projects, focusing on the browser execution environment.
 *
 * **Note:** This interface is not intended for implementation by build script or plugin authors.
 */
@ExperimentalWasmDsl
interface KotlinWasmJsBrowserDsl : KotlinJsBrowserDsl {
    fun devServer(body: Action<KotlinWasmDevServer>)
}
