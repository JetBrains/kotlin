// EXPECTED_REACHABLE_NODES: 1282
// IGNORE_BACKEND: JS_IR, JS_IR_ES6

fun box(): String {
    val a = 'Q'.foo()
    if (a != "number") return "fail1: $a"

    val b = 'W'.bar()
    if (b != "object") return "fail2: $b"

    return "OK"
}

fun Char.foo() = jsTypeOf(this.asDynamic())

fun Any.bar() = jsTypeOf(this.asDynamic())