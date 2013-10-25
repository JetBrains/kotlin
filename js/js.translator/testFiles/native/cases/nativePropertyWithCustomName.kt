package foo

val PACKAGE = "Kotlin.modules.JS_TESTS.foo"

native fun eval(e: String): Any? = noImpl
fun funToString(name: String) = eval("$PACKAGE.$name.toString()") as String

native("\"O\"") val foo: String = noImpl
native("boo") val bar: String = noImpl

class A
native("__proto__") val Any.proto: String = noImpl
native("__proto__") val A.proto: String = noImpl

fun actual(foo: String, native("boo") bar: String) = foo + bar
fun expected(foo: String, boo: String) = foo + boo

fun box(): String {
    val OK = "OK"

    if (foo + bar != OK) return "$foo + $bar != $OK"

    val actualAsString = funToString("actual")
    val expectedAsString = funToString("expected")
    if (actualAsString != expectedAsString) return "$actualAsString != $expectedAsString"
    if (actual("asd", "12345") != "asd12345") return "${actual("asd", "12345")} != \"asd12345\""

    val a = A()
    val any: Any = a
    val protoA = eval("$PACKAGE.A.prototype")
    if (a.proto != any.proto || a.proto != protoA)
        return "a.proto != any.proto /*${a.proto != any.proto}*/ || a.proto != $PACKAGE.A.prototype /*${a.proto != protoA}*/"

    return OK
}
