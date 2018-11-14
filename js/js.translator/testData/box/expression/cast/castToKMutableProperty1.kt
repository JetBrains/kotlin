// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1289
package foo

import kotlin.reflect.KProperty1
import kotlin.reflect.KMutableProperty1

class A {
    var x = 23
}

fun box(): String {
    assertEquals(true, (A::x as Any) is KProperty1<*, *>)
    assertEquals(true, (A::x as Any) is KMutableProperty1<*, *>)
    assertEquals(23, ((A::x as Any) as KProperty1<A, Any>)(A()))
    assertEquals(23, ((A::x as Any) as KMutableProperty1<A, Any>)(A()))
    assertEquals(false, (23 as Any) is KProperty1<*, *>)
    assertEquals(false, (23 as Any) is KMutableProperty1<*, *>)
    assertEquals(false, ({ A().x } as Any) is KMutableProperty1<*, *>)

    return "OK"
}