// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1235
class X(private val x: String) {
    operator fun getValue(thisRef: Any?, property: Any): String = x
}

class C {
    @JsName("a") val `;`: String by X("foo")

    private val `.`: String by X("bar")

    private val `@`: String by X("baz")

    fun bar(): String = `.`

    fun baz(): String = `@`
}

fun box(): String {
    val c = C()
    if (c.`;` != "foo") return "fail1: ${c.`;`}"
    if (c.bar() != "bar") return "fail2: ${c.bar()}"
    if (c.baz() != "baz") return "fail3: ${c.baz()}"

    return "OK"
}