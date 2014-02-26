package foo

val PACKAGE = "Kotlin.modules.JS_TESTS.foo"

native fun eval(e: String): Any? = noImpl

class A
native val Any.__proto__: String = noImpl
native val A.__proto__: String = noImpl

fun box(): String {
    val a = A()
    val any: Any = a
    val protoA = eval("$PACKAGE.A.prototype")
    if (a.__proto__ != any.__proto__ || a.__proto__ != protoA)
        return "a.__proto__ != any.__proto__ /*${a.__proto__ != any.__proto__}*/ || a.__proto__ != $PACKAGE.A.prototype /*${a.__proto__ != protoA}*/"

    return "OK"
}
