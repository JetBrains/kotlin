// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1115
package foo

// CHECK_CONTAINS_NO_CALLS: test

inline fun <T> block(f: () -> T): T = f()

fun test() = block(fun(): Int { return 23 })

fun box(): String {
    assertEquals(23, test())
    return "OK"
}
