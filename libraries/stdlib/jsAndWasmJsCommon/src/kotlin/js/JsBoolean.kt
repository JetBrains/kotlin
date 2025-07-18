/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

/** JavaScript primitive boolean for WasmJs interop */
@ExperimentalWasmJsInterop
public expect class JsBoolean : JsAny

@ExperimentalWasmJsInterop
public expect fun JsBoolean.toBoolean(): Boolean

@ExperimentalWasmJsInterop
public expect fun Boolean.toJsBoolean(): JsBoolean