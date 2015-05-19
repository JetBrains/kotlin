import utils.*

// CHECK_CONTAINS_NO_CALLS: test

fun test(s: String): String = log(s + ";")

fun box(): String {
    assertEquals("a;", test("a"))
    assertEquals("a;b;", test("b"))

    return "OK"
}