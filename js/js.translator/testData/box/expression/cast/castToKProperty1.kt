// EXPECTED_REACHABLE_NODES: 498
package foo

import kotlin.reflect.KProperty1

class A {
    val x = 23
}

fun box(): String {
    assertEquals(true, (A::x as Any) is KProperty1<*, *>)
    assertEquals(23, ((A::x as Any) as KProperty1<A, Any>)(A()))
    assertEquals(false, (23 as Any) is KProperty1<*, *>)
    assertEquals(false, ({ A().x } as Any) is KProperty1<*, *>)

    return "OK"
}