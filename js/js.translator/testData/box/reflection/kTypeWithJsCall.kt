// KJS_WITH_FULL_RUNTIME
// IGNORE_BACKEND: JS_IR_ES6

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf

// FILE: main.kt
external abstract open class A(
    o: String
) {
    abstract val k: String

    fun test(): String
}

class B(
    o: String
) : A(o) {
    override val k = "K"
}

external fun test(
    klazz: Any
): B

fun toJsClass(kType: KType) = (kType.classifier as KClass<*>)?.js

@ExperimentalStdlibApi
fun box(): String {
    return test(toJsClass(typeOf<B>())!!).test()
}

// FILE: test.js

function test(classType) {
    return new classType ("O")
}

function A(o) {
    this.o = o
}

A.prototype.test = function() {
    return this.o + this.k
}