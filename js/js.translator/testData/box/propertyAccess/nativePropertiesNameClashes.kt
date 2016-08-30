package foo

val PACKAGE = "kotlin.modules.JS_TESTS.foo"

class A
@native val Any.__proto__: String get() = noImpl
@native val A.__proto__: String get() = noImpl

fun box(): String {
    val a = A()
    val any: Any = a
    val protoA = eval("$PACKAGE.A.prototype")
    if (a.__proto__ != any.__proto__ || a.__proto__ != protoA)
        return "a.__proto__ != any.__proto__ /*${a.__proto__ != any.__proto__}*/ || a.__proto__ != $PACKAGE.A.prototype /*${a.__proto__ != protoA}*/"

    return "OK"
}
