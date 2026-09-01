/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.sourceMap

object Base64VLQ {
    // A Base64 VLQ digit can represent 5 bits, so it is base-32.
    private const val VLQ_BASE_SHIFT = 5
    private const val VLQ_BASE = 1 shl VLQ_BASE_SHIFT

    // A mask of bits for a VLQ digit (11111), 31 decimal.
    private const val VLQ_BASE_MASK = VLQ_BASE - 1

    // The continuation bit is the 6th bit.
    private const val VLQ_CONTINUATION_BIT = VLQ_BASE

    @Suppress("SpellCheckingInspection")
    private val BASE64_MAP = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray()

    private fun toVLQSigned(value: Int) =
        if (value < 0) (-value shl 1) + 1 else value shl 1

    // Per ECMA-426 (Section 6.2, VLQUnsignedValue), unsigned VLQ values have no sign bit
    // and no doubling step, unlike toVLQSigned.
    private fun toVLQUnsigned(value: Int) = value

    fun encode(out: StringBuilder, value: Int) {
        encodeVlqDigits(out, toVLQSigned(value))
    }

    fun encodeUnsigned(out: StringBuilder, value: Int) {
        encodeVlqDigits(out, toVLQUnsigned(value))
    }

    private fun encodeVlqDigits(out: StringBuilder, value: Int) {
        @Suppress("NAME_SHADOWING")
        var value = value
        do {
            var digit = value and VLQ_BASE_MASK
            value = value ushr VLQ_BASE_SHIFT
            if (value > 0) {
                digit = digit or VLQ_CONTINUATION_BIT
            }
            out.append(BASE64_MAP[digit])
        } while (value > 0)
    }
}
