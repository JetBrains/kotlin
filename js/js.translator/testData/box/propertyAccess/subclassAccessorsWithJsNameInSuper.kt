// EXPECTED_REACHABLE_NODES: 512
package foo

open class A {
    open val x: Int
        @JsName("getX_") get() = 23

    open var y: Int = 0
        @JsName("getY_") get() = field + 10
        @JsName("setY_") set(value) {
            field = value
        }
}

interface C {
    @get:JsName("getZ_") val z: Int
}

class B : A(), C {
    override val x: Int
        get() = 42

    override var y: Int
        get() = super.y + 5
        set(value) {
            super.y = value + 2
        }

    override val z = 55
}

fun getPackage() = js("return JS_TESTS.foo")

fun box(): String {
    val a = B()

    assertEquals(42, a.x)
    assertEquals(15, a.y)
    a.y = 13
    assertEquals(30, a.y)
    assertEquals(55, a.z)

    val d: dynamic = B()

    assertEquals(42, d.getX_())
    assertEquals(15, d.getY_())
    d.setY_(13)
    assertEquals(30, d.getY_())
    assertEquals(55, d.getZ_())

    return "OK"
}