// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1283
// CHECK_NOT_CALLED_IN_SCOPE: function=toBoxedChar scope=box$lambda
// CHECK_CALLED_IN_SCOPE: function=unboxChar scope=box$lambda
// CHECK_CALLED_IN_SCOPE: function=toBoxedChar scope=box
// CHECK_NOT_CALLED_IN_SCOPE: function=unboxChar scope=box

fun <T> bar(x: T, y: (T) -> Boolean): Boolean = y(x) && jsTypeOf(x.asDynamic()) != "number"

fun typeOf(x: dynamic) = js("typeof x")

fun box(): String {
    val f = { x: Char ->
        val a: Char = x
        val b: Any = x
        typeOf(a) == "number" && typeOf(b) == "object"
    }

    if (!f('Q')) return "fail1"
    if (!bar('W', f)) return "fail2"

    return "OK"
}