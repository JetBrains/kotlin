package kotlin.collections

fun Int.highestOneBit() : Int {
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

fun Int.numberOfLeadingZeros() : Int {
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