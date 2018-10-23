// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1282
import kotlin.js.*

private fun isOrdinaryObject(o: Any?): Boolean = jsTypeOf(o) == "object" && Object.getPrototypeOf(o).`constructor` === Any::class.js

external class Object {
    companion object {
        fun getPrototypeOf(x: Any?): dynamic

        fun keys(x: Any): Array<String>
    }
}

fun box(): String {
    val a = js("{}")
    if (!isOrdinaryObject(a)) return "fail: a is not an object"
    if (Object.keys(a).size != 0) return "fail: a should not have any properties"

    val b = js("{ foo: 23, bar: 42 }")
    if (!isOrdinaryObject(b)) return "fail: b is not an object"
    if (Object.keys(b).size != 2) return "fail: b should have two properties"
    if (b.foo != 23) return "fail: b.foo == ${b.foo}"
    if (b.bar != 42) return "fail: b.bar == ${b.bar}"

    return "OK"
}