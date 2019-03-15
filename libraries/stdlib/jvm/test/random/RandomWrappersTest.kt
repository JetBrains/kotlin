/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package test.random

import kotlin.random.*
import kotlin.test.*

class RandomWrappersTest {
    @Test
    fun kotlinRandomAsJavaRandom() {
        val expect = Random(42)
        val actual = Random(42)

        val actualJava = actual.asJavaRandom()

        repeat(10) {
            assertEquals(expect.nextInt(), actualJava.nextInt())
            assertEquals(expect.nextInt(100), actualJava.nextInt(100))
            assertEquals(expect.nextLong(), actualJava.nextLong())
            assertEquals(expect.nextDouble(), actualJava.nextDouble())
            assertEquals(expect.nextFloat(), actualJava.nextFloat())
            assertEquals(expect.nextBoolean(), actualJava.nextBoolean())
        }

        assertSame(actual, actualJava.asKotlinRandom())

        assertFailsWith<UnsupportedOperationException> { actualJava.setSeed(1L) }

        val defaultAsJava = Random.asJavaRandom()
        assertFailsWith<UnsupportedOperationException> { defaultAsJava.setSeed(1L) }
    }

    @Test
    fun javaRandomAsKotlinRandom() {
        val expect = java.util.Random(42L)
        val actual = java.util.Random(42L)

        val actualKotlin = actual.asKotlinRandom()

        repeat(10) {
            assertEquals(expect.nextInt(), actualKotlin.nextInt())
            assertEquals(expect.nextInt(100), actualKotlin.nextInt(100))
            assertEquals(expect.nextLong(), actualKotlin.nextLong())
            assertEquals(expect.nextDouble(), actualKotlin.nextDouble())
            assertEquals(expect.nextFloat(), actualKotlin.nextFloat())
            assertEquals(expect.nextBoolean(), actualKotlin.nextBoolean())
        }

        assertSame(actual, actualKotlin.asJavaRandom())
    }
}