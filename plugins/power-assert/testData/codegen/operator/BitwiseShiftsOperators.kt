fun box(): String = runAll(
    "test1" to { test1() },
    "test2" to { test2(0b10011, 0b11110) },
    "test3" to { test3() },
    "test4" to { test4() },
    "test5" to { test5(null) },
)

fun test1() {
    assert(0b110011 shl 2 == 0b11011100)
}

fun test2(a: Int, b: Int) {
    assert(a shr b == 0b10110)
}

fun test3() {
    assert(-0b1100110011 ushr 22 == 0b1111110111)
}

fun test4() {
    assert(5 shl 3 + 4 shr 1 == 9)
}

fun test5(x: Int?) {
    assert(x ?: 2 shl 1 == 1)
}