// EXPECTED_REACHABLE_NODES: 1276

class A(@JsName("x") val x: Char)

fun typeOf(x: dynamic): String = js("typeof x")

fun box(): String {
    val a = A('0')

    var r = typeOf(a.asDynamic().x)
    if (r != "number") return "fail1: $r"

    r = typeOf(a.x)
    if (r != "number") return "fail2: $r"

    return "OK"
}