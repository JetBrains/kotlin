/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

/** JavaScript primitive bigint for WasmJs interop */
@ExperimentalWasmJsInterop
public expect class JsBigInt : JsAny

@ExperimentalWasmJsInterop
public expect fun JsBigInt.toLong(): Long

@ExperimentalWasmJsInterop
public expect fun Long.toJsBigInt(): JsBigInt