package foo

import kotlin.reflect.KProperty

class A {
    @JsName("xx") val x: Int by B()
}

class B {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Int = 23
}

fun box(): String {
    val a = A()
    assertEquals(23, a.x)

    val d: dynamic = a
    assertEquals(23, d.xx)

    return "OK"
}