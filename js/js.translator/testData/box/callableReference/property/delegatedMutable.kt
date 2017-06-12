// EXPECTED_REACHABLE_NODES: 504
// This test was adapted from compiler/testData/codegen/box/callableReference/property/.
package foo

import kotlin.reflect.KProperty

object Delegate {
    var value = "lol"

    operator fun getValue(instance: Any?, data: KProperty<*>): String {
        return value
    }

    operator fun setValue(instance: Any?, data: KProperty<*>, newValue: String) {
        value = newValue
    }
}

var result: String by Delegate

fun box(): String {
    val f = ::result
    if (f.get() != "lol") return "Fail 1: {$f.get()}"
    Delegate.value = "rofl"
    if (f.get() != "rofl") return "Fail 2: {$f.get()}"
    f.set("OK")
    return f.get()
}
