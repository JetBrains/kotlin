// EXPECTED_REACHABLE_NODES: 491
package foo

class A(val c: Int) {
}


operator fun A.inc() = A(5)
operator fun A.dec() = A(10)

fun box(): String {
    var a = A(1)

    if ((++a).c != 5) return "fail1"
    if ((a++).c != 5) return "fail2"
    if ((--a).c != 10) return "fail3"
    if ((a--).c != 10) return "fail4"

    return "OK"
}
