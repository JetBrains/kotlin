/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections

/**
 * A set of `UByte` values, backed by a 256-bit mask stored in four [Long]s.
 *
 * Used to detect duplicates in byte-domain arrays without allocating a hash-based set.
 */
internal class UByteValueSet {
    private val words = LongArray(4)

    /** Adds [value] to this set; returns `false` if it was already present. */
    fun add(value: UByte): Boolean {
        val index = value.toInt()
        val mask = 1L shl (index and 0x3F)
        val wordIndex = index shr 6
        if (words[wordIndex] and mask != 0L) return false
        words[wordIndex] = words[wordIndex] or mask
        return true
    }
}
