// EXPECTED_REACHABLE_NODES: 491
// FILE: castToTypeParamBoundedByNativeInterface.kt
external interface I {
    fun foo(): String
}

interface J {
    fun bar(): String
}

external abstract class B() : I

external class A(x: String) : B {
    override fun foo(): String = definedExternally
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