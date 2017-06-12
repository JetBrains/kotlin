// EXPECTED_REACHABLE_NODES: 491
package foo


class A(t: Int) {
    var i = t
    infix operator fun compareTo(other: A) = (this.i - other.i)
}

fun box(): String {
    if (A(3) compareTo A(2) <= 0) return "fail1"
    if (A(2) compareTo A(2) != 0) return "fail2"
    if (A(1) compareTo A(0) <= 0) return "fail3"
    if (A(3) compareTo A(4) >= 0) return "fail4"
    if (A(0) compareTo A(100) >= 0) return "fail5"
    return "OK"
}
