/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("EXPECT_ACTUAL_INCOMPATIBLE_CLASS_KIND", "EXPECT_ACTUAL_INCOMPATIBLE_SUPERTYPES")
package kotlin.js

@SinceKotlin("2.2")
@ExperimentalWasmJsInterop
public actual typealias JsPromiseError = JsAny

@ExperimentalWasmJsInterop
@SinceKotlin("2.2")
public actual fun JsPromiseError.asJsException(): JsException =
    JsException(this)
