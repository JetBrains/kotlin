// EXPECTED_REACHABLE_NODES: 499
package foo

fun test(fn: Any?): Function0<Int> =
        fn as Function0<Int>

fun box(): String {
    val get11: Any? = { 11 }
    assertEquals(11, test(get11)(), "get11")
    failsClassCast("null") { test(null)() }
    failsClassCast("object {}") { test(object {})() }

    return "OK"
}