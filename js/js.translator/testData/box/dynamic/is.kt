// EXPECTED_REACHABLE_NODES: 505
// FILE: is.kt
package foo

class A

external class B

fun box(): String {
    val a: dynamic = A()
    testTrue { a is A }
    testFalse { a is B }

    val b: dynamic = B()
    testTrue { b is B }
    testFalse { b is A }

    return "OK"
}

// FILE: is.js
function B() {
}