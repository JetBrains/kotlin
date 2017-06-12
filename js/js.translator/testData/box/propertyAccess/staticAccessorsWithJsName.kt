// EXPECTED_REACHABLE_NODES: 497
package foo

val x: Int
    @JsName("getX_") get() = 23

var y: Int = 0
    @JsName("getY_") get() = field + 10
    @JsName("setY_") set(value) {
        field = value
    }


fun getPackage() = js("return JS_TESTS.foo")

fun box(): String {
    assertEquals(23, x)
    assertEquals(10, y)
    y = 13
    assertEquals(23, y)

    y = 0
    val d = getPackage()

    assertEquals(23, d.getX_())
    assertEquals(10, d.getY_())
    d.setY_(13)
    assertEquals(23, d.getY_())

    return "OK"
}