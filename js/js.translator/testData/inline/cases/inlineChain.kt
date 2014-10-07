package foo

// CHECK_CONTAINS_NO_CALLS: squareMultipliedByTwo

inline fun inline1(a: Int): Int {
    return a
}

inline fun inline2(a: Int): Int {
    return inline1(a) + inline1(a)
}

inline fun inline3(a: Int): Int {
    return inline1(a) * inline2(a)
}

fun squareMultipliedByTwo(a: Int): Int {
    return inline3(a)
}

fun box(): String {
    assertEquals(2, squareMultipliedByTwo(1))
    assertEquals(18, squareMultipliedByTwo(3))
    assertEquals(32, squareMultipliedByTwo(4))

    return "OK"
}