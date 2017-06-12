// EXPECTED_REACHABLE_NODES: 490
// FILE: main.kt

package foo

external class A {
    class B
}

inline fun getA() = A::class
inline fun getB() = foo<A.B>()

inline fun <reified T : Any> foo() = T::class

fun box(): String {
    if (getA() != A::class) return "fail1"
    if (getB() != A.B::class) return "fail2"

    return "OK"
}

// FILE: native.js

function A() {}
A.B = function B() {}
