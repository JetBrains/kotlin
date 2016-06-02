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

class B : A() {
    override val x: Int
        get() = 42

    override var y: Int
        get() = super.y + 5
        set(value) {
            super.y = value + 2
        }
}

fun getPackage() = js("return Kotlin.modules.JS_TESTS.foo")

fun box(): String {
    val a = B()

    assertEquals(42, a.x)
    assertEquals(15, a.y)
    a.y = 13
    assertEquals(30, a.y)

    val d: dynamic = B()

    assertEquals(42, d.getX_())
    assertEquals(15, d.getY_())
    d.setY_(13)
    assertEquals(30, d.getY_())

    return "OK"
}