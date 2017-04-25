// EXPECTED_REACHABLE_NODES: 492
package foo

// CHECK_CONTAINS_NO_CALLS: test

inline fun <T> block(f: () -> T): T = f()

fun test() = block(fun(): Int { return 23 })

fun box(): String {
    assertEquals(23, test())
    return "OK"
}
