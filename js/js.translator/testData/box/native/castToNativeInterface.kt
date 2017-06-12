// EXPECTED_REACHABLE_NODES: 489
// FILE: castToNativeInterface.kt
external interface I {
    fun foo(): String
}

external class A(x: String) : I {
    override fun foo(): String = definedExternally
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