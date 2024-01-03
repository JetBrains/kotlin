/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
// !LANGUAGE: -ProhibitComparisonOfIncompatibleEnums

import kotlin.test.*

enum class EnumA {
    A, B
}

enum class EnumB {
    B
}

fun box(): String {
    if (!(EnumA.A == EnumA.A))
        return "FAIL: A must equal A"
    if (EnumA.A == EnumA.B)
        return "FAIL: A.A must not equal A.B"
    if (EnumA.A == EnumB.B)
        return "FAIL: A.A must not equal B.B"

    return "OK"
}
