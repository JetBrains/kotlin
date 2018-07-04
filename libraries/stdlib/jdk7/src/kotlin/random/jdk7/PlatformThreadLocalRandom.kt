/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.random.jdk7

import java.util.concurrent.ThreadLocalRandom

@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "CANNOT_OVERRIDE_INVISIBLE_MEMBER")
internal class PlatformThreadLocalRandom : kotlin.random.AbstractPlatformRandom() {
    override val impl: ThreadLocalRandom get() = ThreadLocalRandom.current()

    override fun nextInt(origin: Int, bound: Int): Int = impl.nextInt(origin, bound)
    override fun nextLong(bound: Long): Long = impl.nextLong(bound)
    override fun nextLong(origin: Long, bound: Long): Long = impl.nextLong(origin, bound)
    override fun nextDouble(bound: Double): Double = impl.nextDouble(bound)
    override fun nextDouble(origin: Double, bound: Double): Double = impl.nextDouble(origin, bound)
}