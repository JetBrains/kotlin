// EXPECTED_REACHABLE_NODES: 491
// FILE: a.kt
package a.foo

import b.foo.*

fun box() = (A().tadada(A()))


// FILE: b.kt
package b.foo

class A() {
    fun tadada(a: A) = "OK"
}