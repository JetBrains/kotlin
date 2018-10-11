// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1283
class A(val x: Char)

fun typeOf(x: dynamic): String = js("typeof x")

fun box(): String {
    val a = A('0')

    var r = typeOf(a.asDynamic().x)
    if (r != "object") return "fail1: $r"

    r = typeOf(a.x)
    if (r != "number") return "fail2: $r"

    return "OK"
}