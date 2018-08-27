/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.collections

internal fun Int.highestOneBit() : Int {
    var index = 31

    while (index >= 0) {
        var mask = (1 shl index)
        if ((mask and this) != 0) {
            return mask
        }
        index--
    }
    return 0
}

internal fun Int.numberOfLeadingZeros() : Int {
    var index = 31

    while (index >= 0) {
        var mask = (1 shl index)
        if ((mask and this) != 0) {
            return 31 - index
        }
        index--
    }
    return 0
}