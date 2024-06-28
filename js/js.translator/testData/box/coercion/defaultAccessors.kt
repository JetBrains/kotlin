// EXPECTED_REACHABLE_NODES: 1288
// IGNORE_BACKEND: JS_IR, JS_IR_ES6

@JsExport
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

val expectedCharRepresentationInProperty = if (testUtils.isLegacyBackend()) "object" else "number"

fun box(): String {
    val t = jsTypeOf(X.asDynamic().a)
    if (t != expectedCharRepresentationInProperty) return "fail1: $t"

    Y.a = '@'
    Y.a
    if (result != "number") return "fail2: $result"

    return "OK"
}
