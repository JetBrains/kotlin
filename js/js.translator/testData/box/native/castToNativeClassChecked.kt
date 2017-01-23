// FILE: castToNativeClassChecked.kt
@native abstract class S() {
    abstract fun foo(): String
}

@native class A(x: String)  {
    fun foo(): String = definedExternally
}

fun createObject(): Any = A("fail: CCE not thrown")

fun box(): String {
    try {
        return (createObject() as S).foo()
    }
    catch (e: ClassCastException) {
        return "OK"
    }
}

// FILE: castToNativeClassChecked.js
function S() {
}
function A(x) {
    this.x = x;
}
A.prototype.foo = function() {
    return this.x;
}