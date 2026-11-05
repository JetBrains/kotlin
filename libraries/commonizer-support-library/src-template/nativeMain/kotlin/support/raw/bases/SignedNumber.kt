/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package support.raw.bases

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect class SignedNumber : Number {
    /** Include contents of [AnyNumber] */

    operator fun compareTo(other: Byte): Int
    operator fun compareTo(other: Short): Int
    operator fun compareTo(other: Int): Int
    operator fun compareTo(other: Long): Int
    operator fun compareTo(other: Float): Int
    operator fun compareTo(other: Double): Int

    override fun toByte(): Byte
    override fun toShort(): Short
    override fun toInt(): Int
    override fun toLong(): Long
    override fun toFloat(): Float
    override fun toDouble(): Double
}
