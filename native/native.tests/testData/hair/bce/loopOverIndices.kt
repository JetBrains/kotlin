fun sum(a: IntArray): Int {
    var s = 0
    var i = 0
    while (i < a.size) {
        s += a[i]
        i++
    }
    return s
}

fun sumBackwards(a: IntArray): Int {
    var s = 0
    var i = a.size - 1
    while (i >= 0) {
        s += a[i]
        i--
    }
    return s
}

fun main() {
    val a = IntArray(3)
    a[0] = 3; a[1] = 5; a[2] = 7
    if (sum(a) != 15) error("sum(a) = ${sum(a)}, expected 15")
    if (sumBackwards(a) != 15) error("sumBackwards(a) = ${sumBackwards(a)}, expected 15")
    if (sum(IntArray(0)) != 0) error("sum of an empty array must be 0")
    if (sumBackwards(IntArray(0)) != 0) error("sumBackwards of an empty array must be 0")
}
