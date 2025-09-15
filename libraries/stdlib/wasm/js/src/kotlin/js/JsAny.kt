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
@ExperimentalWasmJsInterop
public actual external interface JsAny

/**
 * JavaScript values that are non-shared (in terms of "Shared-Everything Threads" Wasm proposal)
 * even in the "-Xwasm-use-shared-objects" mode.
 * Such types form a separate hierarchy not compatible with `JsAny`, although in "normal" mode all JS types are considered non-shared.
 */
@ExperimentalWasmJsInterop
public actual external interface JsUnshareableAny

/**
 * Cast JsAny to other Js type without runtime check
 */
@WasmNoOpCast
@ExcludedFromCodegen
@ExperimentalWasmJsInterop
public actual fun <T : JsAny> JsAny.unsafeCast(): T =
    implementedAsIntrinsic
