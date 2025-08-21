/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package kotlin.js

/**
 * A wrapper for an exception thrown by a JavaScript code.
 * All exceptions thrown by JS code are signalled to Wasm code as `JsException`.
 *
 */
@ExperimentalWasmJsInterop
@SinceKotlin("2.2")
@Suppress("EXPECT_ACTUAL_IR_INCOMPATIBILITY")
public expect class JsException : Throwable

/**
 * @property thrownValue value thrown by JavaScript; commonly it's an instance of an `Error` or its subclass, but it can be any JavaScript value
 */
@ExperimentalWasmJsInterop
@SinceKotlin("2.2")
public expect val JsException.thrownValue: JsAny?