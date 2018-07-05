/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.random

import kotlin.UnsupportedOperationException
import kotlin.internal.*

/**
 * Creates a [java.util.Random][java.util.Random] instance that uses the specified Kotlin [Random] generator as a randomness source.
 */
@SinceKotlin("1.3")
public fun Random.asJavaRandom(): java.util.Random =
    (this as? AbstractPlatformRandom)?.impl ?: KotlinRandom(this)

/**
 * Creates a Kotlin [Random] instance that uses the specified [java.util.Random][java.util.Random] generator as a randomness source.
 */
@SinceKotlin("1.3")
public fun java.util.Random.asKotlinRandom(): Random =
    (this as? KotlinRandom)?.impl ?: PlatformRandom(this)



@InlineOnly
internal actual inline fun defaultPlatformRandom(): Random =
    IMPLEMENTATIONS.defaultPlatformRandom()

internal actual fun fastLog2(value: Int): Int =
    31 - Integer.numberOfLeadingZeros(value)

internal actual fun doubleFromParts(hi26: Int, low27: Int): Double =
    (hi26.toLong().shl(27) + low27) / (1L shl 53).toDouble()


internal abstract class AbstractPlatformRandom : Random() {
    abstract val impl: java.util.Random

    override fun nextBits(bitCount: Int): Int =
        impl.nextInt().takeUpperBits(bitCount)

    override fun nextInt(): Int = impl.nextInt()
    override fun nextInt(bound: Int): Int = impl.nextInt(bound)
    override fun nextLong(): Long = impl.nextLong()
    override fun nextBoolean(): Boolean = impl.nextBoolean()
    override fun nextDouble(): Double = impl.nextDouble()
    override fun nextFloat(): Float = impl.nextFloat()
    override fun nextBytes(array: ByteArray): ByteArray = array.also { impl.nextBytes(it) }
}

internal class FallbackThreadLocalRandom : AbstractPlatformRandom() {
    private val implStorage = object : ThreadLocal<java.util.Random>() {
        override fun initialValue(): java.util.Random {
            return java.util.Random()
        }
    }
    override val impl: java.util.Random
        get() = implStorage.get()
}

private class PlatformRandom(override val impl: java.util.Random) : AbstractPlatformRandom()

private class KotlinRandom(val impl: Random) : java.util.Random() {
    override fun next(bits: Int): Int = impl.nextBits(bits)
    override fun nextInt(): Int = impl.nextInt()
    override fun nextInt(bound: Int): Int = impl.nextInt(bound)
    override fun nextBoolean(): Boolean = impl.nextBoolean()
    override fun nextLong(): Long = impl.nextLong()
    override fun nextFloat(): Float = impl.nextFloat()
    override fun nextDouble(): Double = impl.nextDouble()

    override fun nextBytes(bytes: ByteArray) {
        impl.nextBytes(bytes)
    }

    override fun setSeed(seed: Long) {
        throw UnsupportedOperationException("Setting seed is not supported.")
    }
}
