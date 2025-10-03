/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

import kotlin.wasm.internal.externalize
import kotlin.wasm.internal.internalize
import kotlin.wasm.internal.unwrapShareable
import kotlin.wasm.internal.wrapShareable

/**
 * A JavaScript value that can be shared between threads in "-Xwasm-use-shared-objects" mode.
 */
@ExperimentalWasmJsInterop
public external interface JsShareableAny : JsAny

/**
 * JavaScript value that can serve as a reference for any Kotlin value.
 *
 * In JavaScript, it behaves like an immutable empty object with a null prototype.
 * When passed back to Kotlin/Wasm, the original value can be retrieved using the [get] method.
 */
@Suppress("WRONG_JS_INTEROP_TYPE")  // Exception to the rule
@ExperimentalWasmJsInterop
public actual sealed external interface JsReference<out T : Any> : JsShareableAny

// TODO maybe inline externalize(!)/internalize(? - more complicated) - unnecessary calls
@ExperimentalWasmJsInterop
public actual fun <T : Any> T.toJsReference(): JsReference<T> = externalize().unsafeCast()

/** Retrieve original Kotlin value from JsReference */
@ExperimentalWasmJsInterop
public actual fun <T : Any> JsReference<T>.get(): T = internalize()
