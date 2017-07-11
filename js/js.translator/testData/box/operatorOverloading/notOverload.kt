// EXPECTED_REACHABLE_NODES: 994
package foo

class A() {

    operator fun not() = "OK"

}

fun box() = !A()