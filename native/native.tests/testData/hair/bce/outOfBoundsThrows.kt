fun offByOne(a: IntArray): Int {
    var s = 0
    var i = 0
    while (i <= a.size) {
        s += a[i]
        i++
    }
    return s
}

fun wrongArray(a: IntArray, b: IntArray, i: Int): Int {
    if (i >= 0) {
        if (i < a.size) return b[i]
    }
    return -1
}

fun upperGuardOnly(a: IntArray, i: Int): Int {
    if (i < a.size) return a[i]
    return -1
}

fun lowerGuardOnly(a: IntArray, i: Int): Int {
    if (i >= 0) return a[i]
    return -1
}

fun pastConstantEnd(): Int {
    val a = IntArray(10)
    return a[10]
}

inline fun expectOutOfBounds(what: String, block: () -> Unit) {
    try {
        block()
    } catch (e: IndexOutOfBoundsException) {
        return
    }
    error("$what: expected IndexOutOfBoundsException")
}

fun main() {
    val a = IntArray(3)
    val b = IntArray(2)
    expectOutOfBounds("offByOne") { offByOne(a) }
    expectOutOfBounds("wrongArray") { wrongArray(a, b, 2) }
    expectOutOfBounds("upperGuardOnly") { upperGuardOnly(a, -1) }
    expectOutOfBounds("lowerGuardOnly") { lowerGuardOnly(a, 3) }
    expectOutOfBounds("pastConstantEnd") { pastConstantEnd() }
}
