/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.random.jdk8

import java.util.concurrent.ThreadLocalRandom

@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "CANNOT_OVERRIDE_INVISIBLE_MEMBER")
internal class PlatformThreadLocalRandom : kotlin.random.AbstractPlatformRandom() {
    // TODO no bridge generated for covariant override
    override val impl: java.util.Random get() = ThreadLocalRandom.current()

    override fun nextInt(origin: Int, bound: Int): Int = ThreadLocalRandom.current().nextInt(origin, bound)
    override fun nextLong(bound: Long): Long = ThreadLocalRandom.current().nextLong(bound)
    override fun nextLong(origin: Long, bound: Long): Long = ThreadLocalRandom.current().nextLong(origin, bound)
    override fun nextDouble(bound: Double): Double = ThreadLocalRandom.current().nextDouble(bound)

//     do not delegate this, as it's buggy in JDK8+ (up to 11 at the moment of writing)
//     override fun nextDouble(origin: Double, bound: Double): Double = ThreadLocalRandom.current().nextDouble(origin, bound)
}