// FILE: castToTypeParamBoundedByNativeInterface.kt
@native interface I {
    fun foo(): String
}

interface J {
    fun bar(): String
}

@native abstract class B() : I

@native class A(x: String) : B() {
    override fun foo(): String = noImpl
}

fun createObject(): Any = A("OK")

fun <T> castToI(o: Any): T where T : I, T : B = o as T

fun box() = castToI<A>(createObject()).foo()

// FILE: castToTypeParamBoundedByNativeInterface.js
function B() {
}

function A(x) {
    this.x = x;
}
A.prototype = Object.create(B.prototype);
A.prototype.foo = function() {
    return this.x;
}