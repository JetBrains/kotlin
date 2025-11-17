/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.wasm.internal

@JsName("String")
internal actual external fun itoa32(inputValue: Int): String

@JsName("String")
internal actual external fun utoa32(inputValue: UInt): String

@JsName("String")
internal actual external fun itoa64(inputValue: Long): String

@JsName("String")
internal actual external fun utoa64(inputValue: ULong): String
