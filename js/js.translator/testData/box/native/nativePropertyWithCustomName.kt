package foo

// TODO: this feature is deprecated, remove this test when either @native annotation or its parameter get eliminated.

internal @native("\"O\"") val foo: String = noImpl
internal @native("boo") val bar: String = noImpl

internal class A
internal @native("__proto__") val Any.proto: String get() = noImpl
internal @native("__proto__") val A.proto: String get() = noImpl

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
    val protoA = A::class.js.asDynamic().prototype
    if (a.proto != any.proto || a.proto != protoA)
        return "a.proto != any.proto /*${a.proto != any.proto}*/ || a.proto != A.prototype /*${a.proto != protoA}*/"

    return OK
}
