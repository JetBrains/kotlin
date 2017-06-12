// EXPECTED_REACHABLE_NODES: 499
// FILE: nativeNativeKotlin.kt

package foo

external open class A {
    fun foo(): String
}

external open class B : A {
    fun bar(): String
}

class C : B()

fun box(): String {
    val c = C()

    assertEquals("A.foo", c.foo())
    assertEquals("B.bar", c.bar())

    return "OK"
}

// FILE: nativeNativeKotlin.js

function A() {

}

A.prototype.foo = function () {
    return "A.foo"
};

function B() {

}

B.prototype = Object.create(A.prototype);
B.prototype.constructor = B;

B.prototype.bar = function () {
    return "B.bar"
};