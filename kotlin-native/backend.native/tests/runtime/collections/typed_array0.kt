/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

package runtime.collections.typed_array0

import kotlin.test.*

@Test fun runTest() {
    // Those tests assume little endian bit ordering.
    val array = ByteArray(42)
    array.setLongAt(5, 0x1234_5678_9abc_def0)

    expect(0x1234_5678_9abc_def0) { array.getLongAt(5) }
    expect(0xdef0.toInt()) { array.getCharAt(5).toInt() }
    expect(0x9abc.toShort()) { array.getShortAt(7) }
    expect(0x1234_5678) { array.getIntAt(9) }
    expect(0xdef0_0000u) { array.getUIntAt(3) }
    expect(0xf0_00u) { array.getUShortAt(4) }
    expect(0xf0u) { array.getUByteAt(5) }
    expect(0x1234_5678_9abcuL) { array.getULongAt(7) }

    array.setIntAt(2, 0x40100000)
    expect(2.25f) { array.getFloatAt(2) }
    array.setLongAt(11, 0x400c_0000_0000_0000)
    expect(3.5) { array.getDoubleAt(11) }

    println("OK")
}