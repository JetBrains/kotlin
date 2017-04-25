// EXPECTED_REACHABLE_NODES: 500
package foo

class A {
    val x: Int
        @JsName("getX_") get() = 23

    var y: Int = 0
        @JsName("getY_") get() = field + 10
        @JsName("setY_") set(value) {
            field = value
        }
}

val A.z: Int
    @JsName("getZ_") get() = 42

fun getPackage() = js("return JS_TESTS.foo")

fun box(): String {
    val a = A()

    assertEquals(23, a.x)
    assertEquals(10, a.y)
    a.y = 13
    assertEquals(23, a.y)
    assertEquals(42, a.z)

    val d: dynamic = A()

    assertEquals(23, d.getX_())
    assertEquals(10, d.getY_())
    d.setY_(13)
    assertEquals(23, d.getY_())
    assertEquals(42, getPackage().getZ_(d))

    return "OK"
}