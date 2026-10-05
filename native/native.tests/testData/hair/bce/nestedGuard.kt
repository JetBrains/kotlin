fun getOr(a: IntArray, i: Int): Int {
    if (i >= 0) {
        if (i < a.size) return a[i]
    }
    return -1
}

fun main() {
    val a = IntArray(3)
    a[0] = 3; a[1] = 5; a[2] = 7
    if (getOr(a, 0) != 3) error("getOr(a, 0) = ${getOr(a, 0)}, expected 3")
    if (getOr(a, 2) != 7) error("getOr(a, 2) = ${getOr(a, 2)}, expected 7")
    if (getOr(a, 3) != -1) error("getOr(a, 3) = ${getOr(a, 3)}, expected -1")
    if (getOr(a, -1) != -1) error("getOr(a, -1) = ${getOr(a, -1)}, expected -1")
}
