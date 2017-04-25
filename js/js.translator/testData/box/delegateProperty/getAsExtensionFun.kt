// EXPECTED_REACHABLE_NODES: 496
package foo

import kotlin.reflect.KProperty

class Delegate {
}

operator fun Delegate.getValue(t: Any?, p: KProperty<*>): Int = 1

class A {
    val prop: Int by Delegate()
}

fun box(): String {
    return if (A().prop == 1) "OK" else "fail"
}
