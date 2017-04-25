// EXPECTED_REACHABLE_NODES: 497
package foo

import kotlin.reflect.KProperty0

val x = 23

fun box(): String {
    assertEquals(true, (::x as Any) is KProperty0<*>)
    assertEquals(23, ((::x as Any) as KProperty0<Any>)())
    assertEquals(false, (23 as Any) is KProperty0<*>)
    assertEquals(false, ({ x } as Any) is KProperty0<*>)

    return "OK"
}