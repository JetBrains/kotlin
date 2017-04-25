// EXPECTED_REACHABLE_NODES: 492
// http://youtrack.jetbrains.com/issue/KT-5320
// KT-5320 Invalid JS code generated for typecast inside ternary operator

package foo

fun test(x: Any?): Int = if (x as Boolean) 1 else 2;

fun box(): String {
    assertEquals(1, test(true), "true")
    assertEquals(2, test(false), "false")
    assertEquals("OK", if (if (0 < 1) false else true) "Not OK" else "OK")

    return "OK"
}