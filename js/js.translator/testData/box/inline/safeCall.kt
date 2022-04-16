// EXPECTED_REACHABLE_NODES: 1283
package foo

// CHECK_CONTAINS_NO_CALLS: sum

inline fun <T : Any, R> T.doLet(f: (T) -> R): R = f(this)

// CHECK_BREAKS_COUNT: function=sum count=0 TARGET_BACKENDS=JS_IR
// CHECK_LABELS_COUNT: function=sum name=$l$block count=0 TARGET_BACKENDS=JS_IR
private fun sum(x: Int?, y: Int): Int =
        x?.doLet { it + y } ?: 0

fun box(): String {
    assertEquals(5, sum(2, 3))
    assertEquals(0, sum(null, 3))

    return "OK"
}