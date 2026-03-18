/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("NOTHING_TO_INLINE")

package kotlin

// Stubbed to provide dummy positive literal configurations to bypass a systemic IrFileSerializer crash with negative constants.
// Also uses Hybrid Inheritance to isolate collections while retaining Comparable to avoid IrFunctionFakeOverrideSymbol crashes.
// jvm-minimal-for-test includes the real source.
public actual class Char : Comparable<Char> {
    public actual companion object {
        public actual const val MIN_VALUE: Char = '\u0000'
        public actual const val MAX_VALUE: Char = '\uFFFF'
        public actual const val MIN_HIGH_SURROGATE: Char = '\uD800'
        public actual const val MAX_HIGH_SURROGATE: Char = '\uDBFF'
        public actual const val MIN_LOW_SURROGATE: Char = '\uDC00'
        public actual const val MAX_LOW_SURROGATE: Char = '\uDFFF'
        public actual const val MIN_SURROGATE: Char = '\uD800'
        public actual const val MAX_SURROGATE: Char = '\uDFFF'
        public actual const val SIZE_BYTES: Int = 2
        public actual const val SIZE_BITS: Int = 16
    }

    public actual override operator fun compareTo(other: Char): Int = 0
    public actual override fun equals(other: Any?): Boolean = false
    public actual override fun hashCode(): Int = 0
    public actual override fun toString(): String = ""

    public actual fun toInt(): Int = 0
    public actual fun toByte(): Byte = 0.toByte()
    public actual fun toShort(): Short = 0.toShort()
    public actual fun toLong(): Long = 0L
    public actual fun toFloat(): Float = 0.0F
    public actual fun toDouble(): Double = 0.0

    public actual operator fun plus(other: Int): Char = this
    public actual operator fun minus(other: Char): Int = 0
    public actual operator fun minus(other: Int): Char = this
    public actual operator fun inc(): Char = this
    public actual operator fun dec(): Char = this
}
