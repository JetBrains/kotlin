// This test was adapted from compiler/testData/codegen/boxWithStdlib/callableReference/property/.
package foo

import kotlin.reflect.KProperty

object Delegate {
    var value = "lol"

    fun getValue(instance: Any?, data: KProperty<*>): String {
        return value
    }

    fun setValue(instance: Any?, data: KProperty<*>, newValue: String) {
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
