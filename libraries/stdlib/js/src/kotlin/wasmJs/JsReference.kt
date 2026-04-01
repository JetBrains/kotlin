/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(ExperimentalJsNoRuntime::class)
package kotlin.js

// TODO: Replace `Any` with `T` as soon as it will be possible
@SinceKotlin("2.2")
@ExperimentalWasmJsInterop
public actual sealed external interface JsReference<out T : Any> : JsAny

@SinceKotlin("2.2")
@ExperimentalWasmJsInterop
@Suppress("NOTHING_TO_INLINE")
public actual inline fun <T : Any> T.toJsReference(): JsReference<T> = unsafeCast<JsReference<T>>()

@SinceKotlin("2.2")
@ExperimentalWasmJsInterop
@Suppress("NOTHING_TO_INLINE")
public actual inline fun <T : Any> JsReference<T>.get(): T = unsafeCast<T>()

@SinceKotlin("2.2")
@ExperimentalWasmJsInterop
@Suppress("NOTHING_TO_INLINE")
@Deprecated("Provided for binary compatibility. JsReference<T>.get() overload should be used instead.", level = DeprecationLevel.HIDDEN)
public inline fun <T : Any> Any.get(): T = unsafeCast<T>()