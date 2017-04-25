// EXPECTED_REACHABLE_NODES: 491
// FILE: a.kt
package a.foo

fun box() = (b.foo.A().tadada(b.foo.A()))


// FILE: b.kt
package b.foo

class A() {
    fun tadada(a: A) = "OK"
}