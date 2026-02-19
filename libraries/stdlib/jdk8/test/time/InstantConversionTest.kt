/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jdk8.time.test

import kotlin.time.*
import kotlin.test.*
import kotlin.random.*
import java.time.Instant as JTInstant

class InstantConversionTest {
    @Test
    fun instant() {
        fun test(seconds: Long, nanosecond: Int) {
            val ktInstant = Instant.fromEpochSeconds(seconds, nanosecond.toLong())
            val jtInstant = JTInstant.ofEpochSecond(seconds, nanosecond.toLong())

            assertEquals(ktInstant, jtInstant.toKotlinInstant())
            assertEquals(jtInstant, ktInstant.toJavaInstant())

            assertEquals(ktInstant, jtInstant.toString().let(Instant::parse))
            assertEquals(jtInstant, ktInstant.toString().let(JTInstant::parse))
        }

        repeat(100) {
            val seconds = Random.nextLong(1_000_000_000_000)
            val nanos = Random.nextInt()
            test(seconds, nanos)
        }
    }
}
