// EXPECTED_REACHABLE_NODES: 1284
package foo

// CHECK_CONTAINS_NO_CALLS: doNothingInt
// CHECK_CONTAINS_NO_CALLS: doNothingStr

internal inline fun <T> doNothing(a: T): T {
    return a
}

// CHECK_BREAKS_COUNT: function=doNothingInt count=0 TARGET_BACKENDS=JS_IR
// CHECK_LABELS_COUNT: function=doNothingInt name=$l$block count=0 TARGET_BACKENDS=JS_IR
internal fun doNothingInt(a: Int): Int {
    return doNothing(a)
}

// CHECK_BREAKS_COUNT: function=doNothingStr count=0 TARGET_BACKENDS=JS_IR
// CHECK_LABELS_COUNT: function=doNothingStr name=$l$block count=0 TARGET_BACKENDS=JS_IR
internal fun doNothingStr(a: String): String {
    return doNothing(a)
}

fun box(): String {
    assertEquals(1, doNothingInt(1))
    assertEquals(2, doNothingInt(2))
    assertEquals("ab", doNothingStr("ab"))
    assertEquals("abc", doNothingStr("abc"))

    return "OK"
}