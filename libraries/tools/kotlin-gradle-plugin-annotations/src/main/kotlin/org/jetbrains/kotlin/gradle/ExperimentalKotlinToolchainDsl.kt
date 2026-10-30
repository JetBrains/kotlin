/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

/** DSL Marker for the experimental toolchain DSL of the WasmJS and WasmWASI tools - D8, Binaryen, Wasmtime and SWC */
@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "This API is experimental and may contain breaking changes in future release."
)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class ExperimentalKotlinToolchainDsl
