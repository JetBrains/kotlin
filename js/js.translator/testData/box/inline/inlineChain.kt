// EXPECTED_REACHABLE_NODES: 492
package foo

// CHECK_CONTAINS_NO_CALLS: squareMultipliedByTwo except=imul

internal inline fun inline1(a: Int): Int {
    return a
}

internal inline fun inline2(a: Int): Int {
    return inline1(a) + inline1(a)
}

internal inline fun inline3(a: Int): Int {
    return inline1(a) * inline2(a)
}

internal fun squareMultipliedByTwo(a: Int): Int {
    return inline3(a)
}

fun box(): String {
    assertEquals(2, squareMultipliedByTwo(1))
    assertEquals(18, squareMultipliedByTwo(3))
    assertEquals(32, squareMultipliedByTwo(4))

    return "OK"
}