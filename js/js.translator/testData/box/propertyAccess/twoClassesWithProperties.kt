// EXPECTED_REACHABLE_NODES: 491
package foo

class A() {
    val a: Int = 1
}

class B() {
    val b: Int = 2
}

fun box(): String {
    if (A().a != 1) return "fail1"
    if (B().b != 2) return "fail2"

    return "OK"
}