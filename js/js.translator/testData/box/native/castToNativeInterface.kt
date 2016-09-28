// FILE: castToNativeInterface.kt
@native interface I {
    fun foo(): String
}

@native class A(x: String) : I {
    override fun foo(): String = noImpl
}

fun createObject(): Any = A("OK")

fun box() = (createObject() as I).foo()

// FILE: castToNativeInterface.js
function A(x) {
    this.x = x;
}
A.prototype.foo = function() {
    return this.x;
}