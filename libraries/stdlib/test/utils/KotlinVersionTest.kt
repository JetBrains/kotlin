/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package test.utils

import kotlin.random.Random
import kotlin.test.*


class KotlinVersionTest {

    @Test fun currentVersion() {
        assertTrue(KotlinVersion.CURRENT.isAtLeast(1, 1))
        assertTrue(KotlinVersion.CURRENT.isAtLeast(1, 1, 0))
        assertTrue(KotlinVersion.CURRENT >= KotlinVersion(1, 1))
        assertTrue(KotlinVersion(1, 1) <= KotlinVersion.CURRENT)

        val anotherCurrent = KotlinVersion.CURRENT.run { KotlinVersion(major, minor, patch) }
        assertEquals(KotlinVersion.CURRENT, anotherCurrent)
        assertEquals(KotlinVersion.CURRENT.hashCode(), anotherCurrent.hashCode())
        assertEquals(0, KotlinVersion.CURRENT.compareTo(anotherCurrent))
    }

    @Test fun componentValidation() {
        for (component in listOf(Int.MIN_VALUE, -1, 0, KotlinVersion.MAX_COMPONENT_VALUE, KotlinVersion.MAX_COMPONENT_VALUE + 1, Int.MAX_VALUE)) {
            for (place in 0..2) {
                val (major, minor, patch) = IntArray(3) { index -> if (index == place) component else 0 }
                if (component in 0..KotlinVersion.MAX_COMPONENT_VALUE) {
                    KotlinVersion(major, minor, patch)
                } else {
                    assertFailsWith<IllegalArgumentException>("Expected $major.$minor.$patch to be invalid version") {
                        KotlinVersion(major, minor, patch)
                    }
                }
            }
        }
    }

    @Test fun versionComparison() {
        val v100 = KotlinVersion(1, 0, 0)
        val v107 = KotlinVersion(1, 0, 7)
        val v110 = KotlinVersion(1, 1, 0)
        val v114 = KotlinVersion(1, 1, 4)
        val v115 = KotlinVersion(1, 1, 50)
        val v120 = KotlinVersion(1, 2, 0)
        val v122 = KotlinVersion(1, 2, 20)
        val v2 = KotlinVersion(2, 0, 0)

        val sorted = listOf(v100, v107, v110, v114, v115, v120, v122, v2)
        for ((prev, next) in sorted.zip(sorted.drop(1))) { // use zipWithNext in 1.2
            val message = "next: $next, prev: $prev"
            assertTrue(next > prev, message)
            assertTrue(next.isAtLeast(prev.major, prev.minor, prev.patch), message)
            assertTrue(next.isAtLeast(prev.major, prev.minor), message)
            assertTrue(next.isAtLeast(next.major, next.minor, next.patch), message)
            assertTrue(next.isAtLeast(next.major, next.minor), message)
            assertFalse(prev.isAtLeast(next.major, next.minor, next.patch), message)
        }
    }

    @Test fun randomVersionComparison() {
        fun randomComponent(): Int = Random.nextInt(KotlinVersion.MAX_COMPONENT_VALUE + 1)
        fun randomVersion() = KotlinVersion(randomComponent(), randomComponent(), randomComponent())
        repeat(1000) {
            val v1 = randomVersion()
            val v2 = randomVersion()
            if (v1.isAtLeast(v2.major, v2.minor, v2.patch))
                assertTrue(v1 >= v2, "Expected version $v1 >= $v2")
        }
    }
}

