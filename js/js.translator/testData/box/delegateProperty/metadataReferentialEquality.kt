// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1302
import kotlin.reflect.KProperty

var lastGeneratedId = 0
val idMap = mutableMapOf<KProperty<*>, Int>()

class MyDelegate(val value: String) {
    operator fun getValue(receiver: Any?, property: KProperty<*>): String {
        val id = idMap.getOrPut(property) { lastGeneratedId++ }
        return "${property.name}:$value:$id"
    }
}

class C {
    val foo by MyDelegate("C")
}

val bar by MyDelegate("toplevel")

fun box(): String {
    val c = C()

    var a = c.foo
    var b = c.foo
    if (a !== b) return "fail: member property referential equality"
    if (!a.startsWith("foo:C:")) return "fail: member property value"

    a = bar
    b = bar
    if (a !== b) return "fail: top level property referential equality"
    if (!a.startsWith("bar:toplevel:")) return "fail: top level property value"

    val baz by MyDelegate("local")
    a = baz
    b = baz
    if (a !== b) return "fail: local property referential equality"
    if (!a.startsWith("baz:local:")) return "fail: local property value"

    return "OK"
}