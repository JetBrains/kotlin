// EXPECTED_REACHABLE_NODES: 497
package foo

class A {
    @get:JsName("getX_") val x = 23

    @get:JsName("getY_") @set:JsName("setY_") var y = 0

    @JsName("z_") var z = 0
}

fun box(): String {
    val a = A()

    assertEquals(23, a.x)
    assertEquals(0, a.y)
    a.y = 42
    assertEquals(42, a.y)
    a.z = 99
    assertEquals(99, a.z)

    val d: dynamic = A()

    assertEquals(23, d.getX_())
    assertEquals(0, d.getY_())
    d.setY_(42)
    assertEquals(42, d.getY_())
    d.z_ = 99
    assertEquals(99, d.z_)

    return "OK"
}