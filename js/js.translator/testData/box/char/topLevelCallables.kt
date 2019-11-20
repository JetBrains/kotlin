// SKIP_MINIFICATION
// IGNORE_BACKEND: JS_IR
@JsName("foo")
@JsExport
fun foo(): Char = '1'

@JsExport
val p1: Char = '2'

@JsExport
var p2: Char = '3'

@JsExport
var p3: Char = '4'
    get() = field + 1
    set(value) {
        field = value + 1
    }

fun box(): String {
    var root = eval("_")

    var r = typeOf(root.foo())
    if (r !== "number") return "fail1: $r"

    r = typeOf(root.p1)
    if (r !== "number") return "fail2: $r"

    r = typeOf(root.p2)
    if (r !== "number") return "fail3: $r"

    r = typeOf(root.p3)
    if (r !== "number") return "fail4: $r"

    return "OK"
}

fun typeOf(x: dynamic): String = js("typeof x")