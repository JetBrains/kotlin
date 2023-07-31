/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

import kotlin.wasm.internal.WasmOp
import kotlin.wasm.internal.implementedAsIntrinsic
import kotlin.wasm.internal.returnArgumentIfItIsKotlinAny

/**
 * JavaScript value that can serve as a reference for any Kotlin value.
 *
 * In JavaScript, it behaves like an immutable empty object with a null prototype.
 * When passed back to Kotlin/Wasm, the original value can be retrieved using the [get] method.
 */
@Suppress("WRONG_JS_INTEROP_TYPE")  // Exception to the rule
public sealed external interface JsReference<out T : Any> : JsAny

@WasmOp(WasmOp.EXTERN_EXTERNALIZE)
public fun <T : Any> T.toJsReference(): JsReference<T> =
    implementedAsIntrinsic

/** Retrieve original Kotlin value from JsReference */
public fun <T : Any> JsReference<T>.get(): T {
    returnArgumentIfItIsKotlinAny()
    throw ClassCastException("JsReference doesn't contain a Kotlin type")
}