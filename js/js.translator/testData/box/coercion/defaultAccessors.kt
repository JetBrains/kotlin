// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1295
interface I {
    val a: Char
}

object X : I {
    override var a = '#'
}

var result = ""

object Y : I {
    override var a = '#'
        get() {
            result = jsTypeOf(field.asDynamic())
            return field
        }
}

fun box(): String {
    val t = jsTypeOf(X.asDynamic().a)
    if (t != "object") return "fail1: $t"

    Y.a = '@'
    Y.a
    if (result != "number") return "fail2: $result"

    return "OK"
}
