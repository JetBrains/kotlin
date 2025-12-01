/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(ExperimentalWasmJsInterop::class)

package kotlin.wasm.internal

@JsFun("Function.prototype.call.bind(Number.prototype.toString)")
internal actual external fun itoa32(inputValue: Int): String

@JsFun("Function.prototype.call.bind(Number.prototype.toString)")
internal actual external fun utoa32(inputValue: UInt): String

@JsFun("Function.prototype.call.bind(BigInt.prototype.toString)")
internal actual external fun itoa64(inputValue: Long): String

@JsFun("Function.prototype.call.bind(BigInt.prototype.toString)")
internal actual external fun utoa64(inputValue: ULong): String
