/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*
import cmacros.*
import kotlinx.cinterop.*

fun main(args: Array<String>) {
    assertEquals("foo", FOO_STRING)
    assertEquals(0, ZERO)
    assertEquals(1, ONE)
    assertEquals(Long.MAX_VALUE, MAX_LONG)
    assertEquals(42, FOURTY_TWO)

    val seventeen: Long = SEVENTEEN
    assertEquals(17L, seventeen)

    val onePointFive: Float = ONE_POINT_FIVE
    val onePointZero: Double = ONE_POINT_ZERO

    assertEquals(1.5f, onePointFive)
    assertEquals(1.0, onePointZero)

    val nullPtr: COpaquePointer? = NULL_PTR
    val voidPtr: COpaquePointer? = VOID_PTR
    val intPtr: CPointer<IntVar>? = INT_PTR
    val ptrSum: CPointer<IntVar>?  = PTR_SUM
    val ptrCall: CPointer<IntVar>? = PTR_CALL

    assertEquals(null, nullPtr)
    assertEquals(1L, voidPtr.rawValue.toLong())
    assertEquals(1L, intPtr.rawValue.toLong())
    assertEquals(PTR_SUM_EXPECTED.toLong(), ptrSum.rawValue.toLong())
    assertEquals(1L, ptrCall.rawValue.toLong())

    assertEquals(42, INT_CALL)
    assertEquals(84, CALL_SUM)
    assertEquals(5, GLOBAL_VAR)

    memScoped {
        val counter = alloc<IntVar>()
        counter.value = 42
        increment(counter.ptr)
        assertEquals(43, counter.value)
    }
}
