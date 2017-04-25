// MINIFICATION_THRESHOLD: 509
package foo

val top = "TOP LEVEL"

fun box(): String {
    // Does't work in Rhino, but should.
    // val v = 1
    // assertEquals(3, eval("v + 2"))

    assertEquals(5, eval("3 + 2"))

    val PACKAGE = "JS_TESTS.foo"
    assertEquals(top, eval("$PACKAGE.top"))

    return "OK"
}