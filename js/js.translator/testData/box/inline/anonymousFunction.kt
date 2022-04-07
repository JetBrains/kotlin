// EXPECTED_REACHABLE_NODES: 1283
package foo

// CHECK_CONTAINS_NO_CALLS: test

inline fun <T> block(f: () -> T): T = f()

// CHECK_BREAKS_COUNT: function=test count=0 TARGET_BACKENDS=JS_IR
// CHECK_LABELS_COUNT: function=test name=$l$block count=0 TARGET_BACKENDS=JS_IR
fun test() = block(fun(): Int { return 23 })

fun box(): String {
    assertEquals(23, test())
    return "OK"
}
