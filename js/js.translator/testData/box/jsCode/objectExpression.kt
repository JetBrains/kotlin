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

    val c = js("{ [(() => 'foo')()]: 'bar' }")
    if (!isOrdinaryObject(c)) return "fail: c is not an object"
    if (Object.keys(c).size != 1) return "fail: c should have one property"
    if (c["foo"] != "bar") return "fail: c['foo'] == ${c["foo"]}"

    val d = js("{ a, b, c }")
    if (!isOrdinaryObject(d)) return "fail: d is not an object"
    if (Object.keys(d).size != 3) return "fail: d should have three properties"
    if (d.a != a) return "fail: d.a == ${d.a}"
    if (d.b != b) return "fail: d.b == ${d.b}"
    if (d.c != c) return "fail: d.c == ${d.c}"

    val e = js("{ ...d }")
    if (!isOrdinaryObject(e)) return "fail: e is not an object"
    if (Object.keys(e).size != 3) return "fail: e should have three properties"
    if (e.a != a) return "fail: e.a == ${e.a}"
    if (e.b != b) return "fail: e.b == ${e.b}"
    if (e.c != c) return "fail: e.c == ${e.c}"

    return "OK"
}