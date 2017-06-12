// EXPECTED_REACHABLE_NODES: 497
package foo

import kotlin.reflect.KProperty0
import kotlin.reflect.KMutableProperty0

var x = 23

fun box(): String {
    assertEquals(true, (::x as Any) is KProperty0<*>)
    assertEquals(true, (::x as Any) is KMutableProperty0<*>)
    assertEquals(23, ((::x as Any) as KProperty0<Any>)())
    assertEquals(23, ((::x as Any) as KMutableProperty0<Any>)())
    assertEquals(false, (23 as Any) is KMutableProperty0<*>)
    assertEquals(false, ({ x } as Any) is KMutableProperty0<*>)

    return "OK"
}