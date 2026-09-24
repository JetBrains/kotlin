/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalWasmJsInterop::class)

package kotlin.wasm.internal

import kotlin.internal.UsedFromCompilerGeneratedCode

@UsedFromCompilerGeneratedCode
internal fun throwValue(t: Throwable): Nothing {
    val v = if (t is JsException) t.thrownValue else t.jsError
    jsThrow(v)
    throw0(v)
}

@UsedFromCompilerGeneratedCode
internal fun getJsError(t: Throwable): JsAny? =
    if (t is JsException) t.thrownValue else t.jsError

// If WebAssembly.JSTag is going to be used for wasm exceptions, use a helper function throwing an exception from JS.
// It's required to work around [an issue in JavaScriptCore](https://bugs.webkit.org/show_bug.cgi?id=297134)
// Otherwise, an empty function is provided, and exceptions will be thrown from wasm code.
@JsFun("wasmTag === wasmJsTag ? (e) => { throw e; } : () => {}")
internal external fun jsThrow(e: JsAny?)

@ExcludedFromCodegen
@UsedFromCompilerGeneratedCode
internal fun throw0(v: JsAny?): Nothing = implementedAsIntrinsic

@UsedFromCompilerGeneratedCode
internal fun getKotlinException(v: JsAny?): Throwable {
    // `kotlinException` is a plain JS property, so it may hold something else than a `Throwable`. Get it as `Any` and use
    // a safe cast instead of a checked one: this falls back to wrapping the value, and keeps the cast failure machinery
    // (`THROW_CCE_WITH_INFO`) out of every program, as this function is always kept.
    val kotlinException: JsReference<Any>? = (v as? JsError)?.kotlinException
    return kotlinException?.get() as? Throwable ?: JsException(v)
}
