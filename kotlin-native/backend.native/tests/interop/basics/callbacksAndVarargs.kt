/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import kotlin.test.*
import kotlinx.cinterop.*
import ccallbacksAndVarargs.*

fun main() {
    testStructCallbacks()
    testVarargs()
    testCallableReferences()
}

fun testStructCallbacks() {
    assertEquals(42, getX(staticCFunction { -> cValue<S> { x = 42 } }))
    applyCallback(cValue { x = 17 }, staticCFunction { it: CValue<S> ->
        assertEquals(17, it.useContents { x })
    })

    assertEquals(66, makeS(66, 111).useContents { x })
}

fun testVarargs() {
    assertEquals(E.ONE, makeE(1))

    getVarargs(
            0,
            true,
            2.toByte(),
            Short.MIN_VALUE,
            42,
            Long.MAX_VALUE,
            3.14f,
            2.71,
            0x1234ABCDL.toCPointer<COpaque>(),
            UByte.MAX_VALUE,
            22.toUShort(),
            111u,
            ULong.MAX_VALUE,
            E.TWO,
            cValue<S> { x = 15 },
            null
    ).useContents {
        assertEquals(1, a1)
        assertEquals(2.toByte(), a2)
        assertEquals(Short.MIN_VALUE, a3)
        assertEquals(42, a4)
        assertEquals(Long.MAX_VALUE, a5)
        assertEquals(3.14f, a6)
        assertEquals(2.71, a7)
        assertEquals(0x1234ABCDL, a8.toLong())
        assertEquals(UByte.MAX_VALUE, a9)
        assertEquals(22.toUShort(), a10)
        assertEquals(111u, a11)
        assertEquals(ULong.MAX_VALUE, a12)
        assertEquals(E.TWO, a13)
        assertEquals(15, a14.x)
        assertEquals(null, a15)
    }
}

fun testCallableReferences() {
    val sumRef = ::sum
    assertEquals(3, sumRef(1, 2))

    val sumPtr = staticCFunction(::sum)
    assertEquals(7, sumPtr(3, 4))
}
