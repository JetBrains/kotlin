/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.wasm.internal

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(s) => Number(s)")
private external fun parseJsNumber(string: String): Double

internal actual fun parseDouble(string: String): Double {
    if (string.isEmpty()) numberFormatError(string)

    return parseJsNumber(string)
}
