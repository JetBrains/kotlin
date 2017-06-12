// EXPECTED_REACHABLE_NODES: 499
package foo

import kotlin.reflect.KProperty

class Delegate {
    var inner = 1
    operator fun getValue(t: Any?, p: KProperty<*>): Int = inner
    operator fun setValue(t: Any?, p: KProperty<*>, i: Int) {
        inner = i
    }
}

fun foo() = Delegate()

class A {
    var prop: Int by foo()
}

fun box(): String {
    val c = A()
    if (c.prop != 1) return "fail get"
    c.prop = 2
    if (c.prop != 2) return "fail set"
    return "OK"
}
