/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(ExperimentalJsNoRuntime::class)
package kotlin.js

/**
 * JavaScript value that can serve as a reference for any Kotlin value.
 *
 * In JavaScript, it behaves like an immutable empty object with a null prototype.
 * When passed back to Kotlin/Wasm, the original value can be retrieved using the [get] method.
 */
@ExperimentalWasmJsInterop
@SinceKotlin("2.2")
@JsNoRuntime
public expect sealed interface JsReference<out T : Any> : JsAny

@ExperimentalWasmJsInterop
@SinceKotlin("2.2")
public expect fun <T : Any> T.toJsReference(): JsReference<T>

/** Retrieve original Kotlin value from JsReference */
@ExperimentalWasmJsInterop
@SinceKotlin("2.2")
public expect fun <T : Any> JsReference<T>.get(): T