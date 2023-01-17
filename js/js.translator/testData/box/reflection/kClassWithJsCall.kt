// IGNORE_BACKEND: JS_IR_ES6
// FILE: main.kt
import kotlin.reflect.KClass

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
) : B

fun toJsClass(klazz: KClass<B>) = klazz.js

fun box(): String {
    return test(toJsClass(B::class)).test()
}

// FILE: test.js

function test(classType) {
    return new classType("O")
}

function A(o) {
    this.o = o
}

A.prototype.test = function() {
    return this.o + this.k
}