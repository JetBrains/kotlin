package foo

internal val PACKAGE = "kotlin.modules.JS_TESTS.foo"

internal @native @JsName("\"O\"") val foo: String = noImpl
internal @native @JsName("boo") val bar: String = noImpl

internal class A
internal fun proto(o: Any?): String = js("o.__proto__")

internal fun actual(foo: String, @native("boo") bar: String) = foo + bar
internal fun expected(foo: String, boo: String) = foo + boo

fun box(): String {
    val OK = "OK"

    if (foo + bar != OK) return "$foo + $bar != $OK"

    val actualAsString = js("actual").toString()
    val expectedAsString = js("expected").toString().replace("expected", "actual")
    if (actualAsString != expectedAsString) return "$actualAsString != $expectedAsString"
    if (actual("asd", "12345") != "asd12345") return "${actual("asd", "12345")} != \"asd12345\""

    val a = A()
    val any: Any = a
    val protoA = js("A.prototype")
    if (proto(a) != proto(any) || proto(a) != protoA)
        return "a.proto != any.proto /*${proto(a) != proto(any)}*/ || a.proto != A.prototype /*${proto(a) != protoA}*/"

    return OK
}
