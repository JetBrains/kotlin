/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

internal typealias BitMask = IntArray

private fun bitMaskWith(activeBit: Int): BitMask {
    val intArray = IntArray((activeBit shr 5) + 1)
    val numberIndex = activeBit shr 5
    val positionInNumber = activeBit and 31
    val numberWithSettledBit = 1 shl positionInNumber
    intArray[numberIndex] = intArray[numberIndex] or numberWithSettledBit
    return intArray
}

internal fun BitMask.isBitSet(possibleActiveBit: Int): Boolean {
    val numberIndex = possibleActiveBit shr 5
    if (numberIndex > size) return false
    val positionInNumber = possibleActiveBit and 31
    val numberWithSettledBit = 1 shl positionInNumber
    return get(numberIndex) and numberWithSettledBit != 0
}

private fun compositeBitMask(capacity: Int, masks: Array<BitMask>): BitMask {
    return IntArray(capacity) { i ->
        var result = 0
        for (mask in masks) {
            if (i < mask.size) {
                result = result or mask[i]
            }
        }
        result
    }
}

internal fun implement(interfaces: Array<dynamic>): BitMask {
    var maxSize = 1
    val masks = js("[]")

    for (i in interfaces) {
        var currentSize = maxSize
        val imask: BitMask? = i.prototype.`$imask$` ?: i.`$imask$`

        if (imask != null) {
            masks.push(imask)
            currentSize = imask.size
        }

        val iid: Int? = i.`$metadata$`.iid
        val iidImask: BitMask? = iid?.let { bitMaskWith(it) }

        if (iidImask != null) {
            masks.push(iidImask)
            currentSize = JsMath.max(currentSize, iidImask.size)
        }

        if (currentSize > maxSize) {
            maxSize = currentSize
        }
    }

    return compositeBitMask(maxSize, masks)
}
