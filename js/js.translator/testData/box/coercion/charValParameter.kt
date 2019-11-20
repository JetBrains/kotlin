// EXPECTED_REACHABLE_NODES: 1276
// IGNORE_BACKEND: JS_IR
class A(val x: Char)

fun typeOf(x: dynamic): String = js("typeof x")

val expectedCharRepresentationInProperty = if (testUtils.isLegacyBackend()) "object" else "number"

fun box(): String {
    val a = A('0')

    var r = typeOf(a.asDynamic().x)
    if (r != expectedCharRepresentationInProperty) return "fail1: $r"

    r = typeOf(a.x)
    if (r != "number") return "fail2: $r"

    return "OK"
}