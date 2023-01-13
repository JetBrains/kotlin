/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

fun testArrayAllocation(size: Int) {
    val arr = IntArray(size)
    // Force a write into the memory.
    // TODO: How to make sure the optimizer never deletes this write?
    arr[size - 1] = 42
    assertEquals(42, arr[size - 1])
}

@Test
fun sanity() {
    // Should always succeed everywhere
    testArrayAllocation(1 shl 10)
}

@Test
fun test() {
    // Will fail on 32 bits.
    testArrayAllocation(1 shl 30)
}
