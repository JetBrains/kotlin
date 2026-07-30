/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

import kotlin.internal.UsedFromCompilerGeneratedCode

@UsedFromCompilerGeneratedCode
internal fun idiv(a: dynamic, b: dynamic): Int {
    if (b === 0) throw ArithmeticException("/ by zero")
    return jsBitwiseOr(a / b, 0)
}

@UsedFromCompilerGeneratedCode
internal fun irem(a: dynamic, b: dynamic): Int {
    if (b === 0) throw ArithmeticException("/ by zero")
    return jsBitwiseOr(a % b, 0)
}
