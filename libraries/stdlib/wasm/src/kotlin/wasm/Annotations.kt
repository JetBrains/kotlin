/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.wasm

/**
 * Marks API related to interoperability with the Wasm platform as experimental.
 *
 * Note that the behavior of annotated API may likely be changed in the future.
 *
 * Usages of such API will be reported as warnings unless an explicit opt-in with
 * the [OptIn] annotation, e.g. `@OptIn(ExperimentalWasmInterop::class)`,
 * or with the `-opt-in=kotlin.js.ExperimentalWasmInterop` compiler option is given.
 */
@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@SinceKotlin("2.2")
@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
public annotation class ExperimentalWasmInterop

/**
 * Imports a function from the given [module] with the given optional [name].
 * The declaration name will be used if the [name] argument is not provided.
 *
 * Can only be used on top-level external functions.
 *
 * The annotated function will be imported into Wasm module without type adapters.
 */
@ExperimentalWasmInterop
@Target(AnnotationTarget.FUNCTION)
public annotation class WasmImport(
    val module: String,
    val name: String = ""
)

/**
 * Exports a function with the given optional [name].
 * The declaration name will be used if the [name] argument is not provided.
 *
 * Can only be used on top-level non-external functions.
 *
 * The annotated function will be exported from Wasm module without type adapters.
 */
@ExperimentalWasmInterop
@Target(AnnotationTarget.FUNCTION)
public annotation class WasmExport(
    val name: String = ""
)

@Target(AnnotationTarget.FUNCTION)
internal annotation class JsBuiltin(
    val module: String,
    val name: String = "",
    val polyfill: String = ""
)