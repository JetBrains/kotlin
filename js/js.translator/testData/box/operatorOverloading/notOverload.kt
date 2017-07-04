// EXPECTED_REACHABLE_NODES: 1378
package foo

class A() {

    operator fun not() = "OK"

}

fun box() = !A()