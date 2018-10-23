// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1296
open class A {
    val foo: Char
        get() = 'X'

    var bar: Char = 'Y'

    val baz: Char = 'Q'

    var mutable: Char = 'W'
        get() {
            typeOfMutable += typeOf(field.asDynamic()) + ";"
            return field + 1
        }
        set(value) {
            typeOfMutable += typeOf(js("value")) + ";" + typeOf(value)
            field = value
        }
}

interface I {
    val foo: Any

    val bar: Any

    val baz: Any

    val mutable: Any
}

class B : A(), I

fun typeOf(x: dynamic): String = js("typeof x")

var typeOfMutable = ""

fun box(): String {
    val a = B()
    val b: I = B()

    val r1 = typeOf(a.foo)
    if (r1 != "number") return "fail1: $r1"

    val r2 = typeOf(b.foo)
    if (r2 != "object") return "fail2: $r2"

    val r3 = typeOf(a.asDynamic().foo)
    if (r3 != "object") return "fail3: $r3"

    val r4 = typeOf(a.asDynamic().bar)
    if (r4 != "object") return "fail4: $r4"

    val r5 = typeOf(a.asDynamic().baz)
    if (r5 != "object") return "fail5: $r5"

    a.bar++
    val r6 = typeOf(a.asDynamic().bar)
    if (r6 != "object") return "fail6: $r6"

    val r7 = typeOf(a.asDynamic().mutable)
    if (r7 != "object") return "fail7: $r7"

    a.mutable = 'E'
    if (typeOfMutable != "number;object;number") return "fail8: $typeOfMutable"

    val r9 = typeOf(a.mutable)
    if (r9 != "number") return "fail9: $r9"

    return "OK"
}