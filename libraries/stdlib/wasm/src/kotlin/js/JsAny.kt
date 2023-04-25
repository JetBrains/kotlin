/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

import kotlin.wasm.internal.ExcludedFromCodegen
import kotlin.wasm.internal.WasmNoOpCast
import kotlin.wasm.internal.implementedAsIntrinsic

/**
 * Any JavaScript value except null or undefined
 */
public external interface JsAny

/**
 * Cast JsAny to other Js type without runtime check
 */
@WasmNoOpCast
@ExcludedFromCodegen
public fun <T : JsAny> JsAny.unsafeCast(): T =
    implementedAsIntrinsic