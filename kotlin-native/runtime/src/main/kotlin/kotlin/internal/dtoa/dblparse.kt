/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.internal.dtoa

import kotlin.native.internal.GCUnsafeCall
import kotlin.native.internal.escapeAnalysis.Escapes

@GCUnsafeCall("Kotlin_native_NumberConverter_bigIntDigitGeneratorInstImpl")
@Escapes.Nothing
internal actual external fun bigIntDigitGeneratorInstImpl(
        results: IntArray,
        uArray: IntArray,
        f: Long,
        e: Int,
        isDenormalized: Boolean,
        mantissaIsZero: Boolean,
        p: Int,
)

@GCUnsafeCall("Kotlin_native_FloatingPointParser_parseDoubleImpl")
@Escapes.Nothing
internal actual external fun parseDoubleImpl(s: String, e: Int): Double