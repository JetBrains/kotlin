/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.wasm.internal

@OptIn(ExperimentalWasmJsInterop::class)
private fun parseJsNumber(string: String): Double =
    js("Number(string)")

private fun String.isNaN(): Boolean = when (this) {
    "NaN", "+NaN", "-NaN" -> true
    else -> false
}

internal actual fun parseDouble(string: String): Double {
    val significantPart = string.trim()
    if (significantPart.length >= 2 && (significantPart[0] == '0' &&
                significantPart[1].lowercaseChar() in listOf('x', 'o', 'b'))
    ) {
        numberFormatError(string)
    }
    val jsNumber = parseJsNumber(significantPart)
    if (jsNumber.isNaN() && !significantPart.isNaN() || jsNumber == 0.0 && significantPart.isBlank())
        numberFormatError(string)
    return jsNumber
}
