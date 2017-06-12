// EXPECTED_REACHABLE_NODES: 502
package foo

import kotlin.reflect.KProperty

class A {
    @JsName("xx") val x: Int by B(23)

    @get:JsName("getYY") val y: Int by B(42)
}

class B(val value: Int) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Int = value
}

fun box(): String {
    val a = A()
    assertEquals(23, a.x)
    assertEquals(42, a.y)

    val d: dynamic = a
    assertEquals(23, d.xx)
    assertEquals(42, d.getYY())

    return "OK"
}