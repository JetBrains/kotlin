// EXPECTED_REACHABLE_NODES: 1283
package foo

// CHECK_CONTAINS_NO_CALLS: squareMultipliedByTwo except=imul

internal inline fun inline1(a: Int): Int {
    return a
}

internal inline fun inline2(a: Int): Int {
    val a1 = inline1(a)
    if (a1 == 0) return 0
    return a1 + inline1(a)
}

internal inline fun inline3(a: Int): Int {
    val i = inline2(a)
    val i1 = inline1(a) * i
    if (i == i1) return 0
    return i1
}

// CHECK_BREAKS_COUNT: function=squareMultipliedByTwo count=2 TARGET_BACKENDS=JS_IR
// CHECK_LABELS_COUNT: function=squareMultipliedByTwo name=$l$block count=1 TARGET_BACKENDS=JS_IR
// CHECK_LABELS_COUNT: function=squareMultipliedByTwo name=$l$block_0 count=1 TARGET_BACKENDS=JS_IR
internal fun squareMultipliedByTwo(a: Int): Int {
    return inline3(a)
}

fun box(): String {
    assertEquals(0, squareMultipliedByTwo(1))
    assertEquals(18, squareMultipliedByTwo(3))
    assertEquals(32, squareMultipliedByTwo(4))

    return "OK"
}
