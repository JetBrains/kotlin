// EXPECTED_REACHABLE_NODES: 994
package foo

class A() {

    operator fun div(other: A) = "OK"

}

fun box() = A() / A()