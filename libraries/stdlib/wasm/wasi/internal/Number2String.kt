/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.wasm.internal

internal actual fun itoa32(inputValue: Int): String {
    if (inputValue == 0) return "0"

    val isNegative = inputValue < 0
    val absValue = if (isNegative) -inputValue else inputValue
    val absValueString = utoa32(absValue.toUInt())

    return if (isNegative) "-$absValueString" else absValueString
}