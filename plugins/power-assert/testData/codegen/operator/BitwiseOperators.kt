fun box(): String = runAll(
    "test1" to { test1() },
    "test2" to { test2(0b10011, 0b11110) },
    "test3" to { test3(0b10011, 0b11110) },
    "test4" to { test4(0b10011, 0b11110) },
    "test5" to { test5() },
    "test6" to { test6(null) },
)

fun test1() {
    assert(true and false)
}

fun test2(a: Int, b: Int) {
    assert(a and b == 0b10110)
}

fun test3(a: Int, b: Int) {
    assert(a or b == 0b10110)
}

fun test4(a: Int, b: Int) {
    assert(a xor b == 0b10110)
}

fun test5() {
    assert(5 and 3 + 4 or 1 == 9)
}

fun test6(x: Int?) {
    assert(x ?: 2 and 1 == 1)
}