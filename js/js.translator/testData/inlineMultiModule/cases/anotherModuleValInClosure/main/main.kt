import utils.*

// CHECK_CONTAINS_NO_CALLS: test_0

internal fun test(s: String): String = log(s + ";")

fun box(): String {
    assertEquals("a;", test("a"))
    assertEquals("a;b;", test("b"))

    return "OK"
}