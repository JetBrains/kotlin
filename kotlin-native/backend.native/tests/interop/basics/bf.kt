/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import bitfields.*
import kotlinx.cinterop.*

fun assertEquals(value1: Any?, value2: Any?) {
    if (value1 != value2)
        throw AssertionError("Expected $value1, got $value2")
}

fun check(s: S, x1: Long, x2: B2, x3: UShort, x4: UInt, x5: Int, x6: Long, x7: E, x8: Boolean) {
    assertEquals(x1, s.x1)
    assertEquals(x1, getX1(s.ptr))

    assertEquals(x2, s.x2)
    assertEquals(x2, getX2(s.ptr))

    assertEquals(x3, s.x3)
    assertEquals(x3, getX3(s.ptr))

    assertEquals(x4, s.x4)
    assertEquals(x4, getX4(s.ptr))

    assertEquals(x5, s.x5)
    assertEquals(x5, getX5(s.ptr))

    assertEquals(x6, s.x6)
    assertEquals(x6, getX6(s.ptr))

    assertEquals(x7, s.x7)
    assertEquals(x7, getX7(s.ptr))

    assertEquals(x8, s.x8)
    assertEquals(x8, getX8(s.ptr))
}

fun assign(s: S, x1: Long, x2: B2, x3: UShort, x4: UInt, x5: Int, x6: Long, x7: E, x8: Boolean) {
    s.x1 = x1
    s.x2 = x2
    s.x3 = x3
    s.x4 = x4
    s.x5 = x5
    s.x6 = x6
    s.x7 = x7
    s.x8 = x8
}

fun assignReversed(s: S, x1: Long, x2: B2, x3: UShort, x4: UInt, x5: Int, x6: Long, x7: E, x8: Boolean) {
    s.x8 = x8
    s.x7 = x7
    s.x6 = x6
    s.x5 = x5
    s.x4 = x4
    s.x3 = x3
    s.x2 = x2
    s.x1 = x1
}

fun test(s: S, x1: Long, x2: B2, x3: UShort, x4: UInt, x5: Int, x6: Long, x7: E, x8: Boolean) {
    assign(s, x1, x2, x3, x4, x5, x6, x7, x8)
    check(s, x1, x2, x3, x4, x5, x6, x7, x8)

    assignReversed(s, x1, x2, x3, x4, x5, x6, x7, x8)
    check(s, x1, x2, x3, x4, x5, x6, x7, x8)

    // Also check with some insignificant bits modified:

    assign(s, x1 + 2, x2, (x3 + 8u).toUShort(), x4 - 16u, x5 + 32, x6 + Long.MIN_VALUE, x7, x8)
    check(s, x1, x2, x3, x4, x5, x6, x7, x8)

    assignReversed(s, x1 + 2, x2, (x3 + 8u).toUShort(), x4 - 16u, x5 + 32, x6 + Long.MIN_VALUE, x7, x8)
    check(s, x1, x2, x3, x4, x5, x6, x7, x8)
}

fun main(args: Array<String>) {
    memScoped {
        val s = alloc<S>()
        for (x1 in -1L..0L)
            for (x2 in B2.values())
                for (x3 in 0..7)
                    for (x4 in uintArrayOf(0u, 6u, 15u))
                        for (x5 in intArrayOf(-16, -2, -1, 0, 5, 15))
                            for (x6 in longArrayOf(Long.MIN_VALUE/2, -1L shl 36, -325L, 0, 1L shl 48, Long.MAX_VALUE/2))
                                for (x7 in E.values())
                                    for (x8 in arrayOf(false, true))
                                        test(s, x1, x2, x3.toUShort(), x4, x5, x6, x7, x8)
    }
}
