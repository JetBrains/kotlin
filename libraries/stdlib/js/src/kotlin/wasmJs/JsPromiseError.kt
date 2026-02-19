/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

import kotlin.internal.InlineOnly

@SinceKotlin("2.2")
@ExperimentalWasmJsInterop
public actual typealias JsPromiseError = Throwable

@InlineOnly
@ExperimentalWasmJsInterop
@SinceKotlin("2.2")
public actual inline fun JsPromiseError.asJsException(): Throwable = this
