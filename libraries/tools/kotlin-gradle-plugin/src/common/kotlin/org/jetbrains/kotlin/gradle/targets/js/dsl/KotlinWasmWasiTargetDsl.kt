/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.dsl

import org.jetbrains.kotlin.gradle.plugin.mpp.HasBinaries
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsBinaryContainer

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
    HasBinaries<KotlinJsBinaryContainer>
