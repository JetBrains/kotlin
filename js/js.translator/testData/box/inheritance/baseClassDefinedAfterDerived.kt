// EXPECTED_REACHABLE_NODES: 496
package foo


class A() : B() {

}

open class B() {

    val a = 3
}

fun box() = if (A().a == 3) "OK" else "fail"