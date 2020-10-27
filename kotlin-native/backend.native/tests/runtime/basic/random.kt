/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.basic.random

import kotlin.collections.*
import kotlin.random.*
import kotlin.system.*
import kotlin.test.*

/**
 * Tests that setting the same seed make random generate the same sequence
 */
private inline fun <reified T> testReproducibility(seed: Long, generator: Random.() -> T) {
    // Reset seed. This will make Random to start a new sequence
    val r1 = Random(seed)
    val first = Array<T>(50, { i -> r1.generator() }).toList()

    // Reset seed and try again
    val r2 = Random(seed)
    val second = Array<T>(50, { i -> r2.generator() }).toList()
    assertTrue(first == second, "FAIL: got different sequences of generated values " +
            "first: $first, second: $second")
}

/**
 * Tests that setting seed makes random generate different sequence.
 */
private inline fun <reified T> testDifference(generator: Random.() -> T) {
    val r1 = Random(12345678L)
    val first = Array<T>(100, { i -> r1.generator() }).toList()

    val r2 = Random(87654321L)
    val second = Array<T>(100, { i -> r2.generator() }).toList()
    assertTrue(first != second, "FAIL: got the same sequence of generated values " +
            "first: $first, second: $second")
}

@Test
fun testInts() {
    testReproducibility(getTimeMillis(), { nextInt() })
    testReproducibility(Long.MAX_VALUE, { nextInt() })
}

@Test
fun testLong() {
    testReproducibility(getTimeMillis(), { nextLong() })
    testReproducibility(Long.MAX_VALUE, { nextLong() })
}

@Test
fun testDiffInt() = testDifference { nextInt() }

@Test
fun testDiffLong() = testDifference { nextLong() }

@Test
fun testNextInt() {
    testReproducibility(getTimeMillis(), { nextInt(1000) })
    testReproducibility(1000L, { nextInt(1024) })
}
