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

    val f = js("{ a, b, c, ...d }")
    if (!isOrdinaryObject(f)) return "fail: f is not an object"
    if (Object.keys(f).size != 3) return "fail: f should have three properties"
    if (f.a != a) return "fail: f.a == ${f.a}"
    if (f.b != b) return "fail: f.b == ${f.b}"
    if (f.c != c) return "fail: f.c == ${f.c}"

    val g = js("{ one: a, two: b, ...d, three: c }")
    if (!isOrdinaryObject(g)) return "fail: g is not an object"
    if (Object.keys(g).size != 6) return "fail: g should have six properties"
    if (g.one != a) return "fail: g.one == ${g.one}"
    if (g.two != b) return "fail: g.two == ${g.two}"
    if (g.three != c) return "fail: g.three == ${g.three}"
    if (g.a != a) return "fail: g.a == ${g.a}"
    if (g.b != b) return "fail: g.b == ${g.b}"
    if (g.c != c) return "fail: g.c == ${g.c}"

    val h = js("{ sayHi(name) { return 'Hello, ' + name } }")
    if (!isOrdinaryObject(h)) return "fail: h is not an object"
    if (Object.keys(h).size != 1) return "fail: h should have one property"
    val greeting = h.sayHi("World")
    if (greeting != "Hello, World") return "fail: h.sayHi('World') == '$greeting'"

    val i = js("{ *oneTwo() { yield 1; yield 2; } }")
    if (!isOrdinaryObject(i)) return "fail: i is not an object"
    if (Object.keys(i).size != 1) return "fail: i siould iave one property"
    val generator = i.oneTwo()
    val one = generator.next().value
    val two = generator.next().value
    if (one != 1) return "fail: i.oneTwo().first == '$one'"
    if (two != 2) return "fail: i.oneTwo().second == '$two'"

    val j = js("""{ 
        sumDefaultB(a, b = 2) { return a + b; },
        sumDefaultAB(a = 1, b = a + 1) { return a + b; },
        sumDefaultComma(a = 1, b = (a, a + 1)) { return a + b; },
        sumDefaultComplex(a = 1, b = (() => a + 1)()) { return a + b; }
    }""")
    if (!isOrdinaryObject(j)) return "fail: j is not an object"
    if (Object.keys(j).size != 4) return "fail: j should have four properties"
    val defaultB = j.sumDefaultB(1)
    if (defaultB != 3) return "fail: j.sumDefaultB(1) == ${defaultB}"
    val defaultAB = j.sumDefaultAB()
    if (defaultAB != 3) return "fail: j.sumDefaultAB() == ${defaultAB}"
    val defaultComma = j.sumDefaultComma()
    if (defaultComma != 3) return "fail: j.sumDefaultComma() == ${defaultComma}"
    val defaultComplex = j.sumDefaultComplex()
    if (defaultComplex != 3) return "fail: j.sumDefaultComplex() == ${defaultComplex}"

    return "OK"
}