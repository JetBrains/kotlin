inline fun bar(arr: IntArray, transform: (Int) -> Int): Int {
    var max = Int.MIN_VALUE
    for (x in arr) {
        val y = transform(x)
        if (y > max) max = y
    }
    return max
}