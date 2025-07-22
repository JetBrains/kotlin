/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("ACTUAL_TYPE_ALIAS_WITH_COMPLEX_SUBSTITUTION", "EXPECT_ACTUAL_INCOMPATIBLE_CLASS_TYPE_PARAMETER_COUNT")
package kotlin.js

// TODO: Replace `Any` with `T` as soon as it will be possible
@SinceKotlin("2.2")
@ExperimentalWasmJsInterop
public actual typealias JsReference<T> = Any

@SinceKotlin("2.2")
@ExperimentalWasmJsInterop
@Suppress("NOTHING_TO_INLINE")
public actual inline fun <T : Any> T.toJsReference(): JsReference<T> = unsafeCast<JsReference<T>>()

@SinceKotlin("2.2")
@ExperimentalWasmJsInterop
@Suppress("NOTHING_TO_INLINE")
public actual inline fun <T : Any> JsReference<T>.get(): T = unsafeCast<T>()
