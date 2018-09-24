// EXPECTED_REACHABLE_NODES: 1284
package foo

class A() {

    operator fun not() = "OK"

}

fun box() = !A()