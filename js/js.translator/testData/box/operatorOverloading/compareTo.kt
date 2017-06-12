// EXPECTED_REACHABLE_NODES: 491
package foo


class A(t: Int) {
    var i = t
    operator fun compareTo(other: A) = (this.i - other.i)
}

fun box(): String {
    if (A(3) <= A(2)) return "fail1"
    if (A(2) < A(2)) return "fail2"
    if (A(1) < A(0)) return "fail3"
    if (A(2) > A(2)) return "fail4"
    if (A(3) > A(4)) return "fail5"
    if (A(0) >= A(100)) return "fail6"

    return "OK"
}
