/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package kotlin.js

@ExperimentalWasmJsInterop
@Suppress("EXPECT_ACTUAL_IR_INCOMPATIBILITY")
public expect sealed interface JsReference<out T : Any> : JsAny

@ExperimentalWasmJsInterop
public expect fun <T : Any> T.toJsReference(): JsReference<T>

/** Retrieve original Kotlin value from JsReference */
@ExperimentalWasmJsInterop
public expect fun <T : Any> JsReference<T>.get(): T