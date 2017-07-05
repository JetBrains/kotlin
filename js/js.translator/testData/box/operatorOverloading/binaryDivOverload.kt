// EXPECTED_REACHABLE_NODES: 1378
package foo

class A() {

    operator fun div(other: A) = "OK"

}

fun box() = A() / A()