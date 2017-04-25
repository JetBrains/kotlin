// EXPECTED_REACHABLE_NODES: 491
package foo

class A() {

    operator fun not() = "OK"

}

fun box() = !A()