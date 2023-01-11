/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

internal fun implement(vararg interfaces: dynamic): BitMask {
    var maxSize = 1
    val masks = js("[]")

    for (i in interfaces) {
        var currentSize = maxSize
        val imask: BitMask? = i.prototype.`$imask$` ?: i.`$imask$`

        if (imask != null) {
            masks.push(imask)
            currentSize = imask.intArray.size
        }

        val iid: Int? = i.`$metadata$`.iid
        val iidImask: BitMask? = iid?.let { BitMask(arrayOf(it)) }

        if (iidImask != null) {
            masks.push(iidImask)
            currentSize = JsMath.max(currentSize, iidImask.intArray.size)
        }

        if (currentSize > maxSize) {
            maxSize = currentSize
        }
    }

    val resultIntArray = IntArray(maxSize) { i ->
        masks.reduce({ acc: Int, it: BitMask ->
            if (i >= it.intArray.size)
                acc
            else
                acc or it.intArray[i]
        }, 0)
    }

    val result = BitMask(emptyArray())
    result.intArray = resultIntArray
    return result
}

internal class BitMask(activeBits: Array<Int>) {
    var intArray: IntArray = run {
        if (activeBits.size == 0) {
            IntArray(0)
        } else {
            val max: Int = JsMath.asDynamic().max.apply(null, activeBits)
            val intArray = IntArray((max shr 5) + 1)
            for (activeBit in activeBits) {
                val numberIndex = activeBit shr 5
                val positionInNumber = activeBit and 31
                val numberWithSettledBit = 1 shl positionInNumber
                intArray[numberIndex] = intArray[numberIndex] or numberWithSettledBit
            }
            intArray
        }
    }

    fun isBitSet(possibleActiveBit: Int): Boolean {
        val numberIndex = possibleActiveBit shr 5
        if (numberIndex > intArray.size) return false
        val positionInNumber = possibleActiveBit and 31
        val numberWithSettledBit = 1 shl positionInNumber
        return intArray[numberIndex] and numberWithSettledBit != 0
    }
}