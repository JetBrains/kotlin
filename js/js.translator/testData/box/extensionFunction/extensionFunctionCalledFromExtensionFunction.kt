// EXPECTED_REACHABLE_NODES: 491
package foo

class A() {

}

fun A.one() = 1
fun A.two() = one() + one()

fun box(): String {
    return if (A().two() == 2) "OK" else "fail"
}