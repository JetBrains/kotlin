package foo

// CHECK_CONTAINS_NO_CALLS: squareMultipliedByTwo

inline fun inline1(a: Int): Int {
    return a
}

inline fun inline2(a: Int): Int {
    val a1 = inline1(a)
    if (a1 == 0) return 0
    return a1 + inline1(a)
}

inline fun inline3(a: Int): Int {
    val i = inline2(a)
    val i1 = inline1(a) * i
    if (i == i1) return 0
    return i1
}

fun squareMultipliedByTwo(a: Int): Int {
    return inline3(a)
}

fun box(): String {
    assertEquals(0, squareMultipliedByTwo(1))
    assertEquals(18, squareMultipliedByTwo(3))
    assertEquals(32, squareMultipliedByTwo(4))

    return "OK"
}
