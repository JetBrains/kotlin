package foo


class A(t: Int) {
    var i = t
    fun compareTo(other: A) = (this.i - other.i)
}

fun box(): Boolean =
    (A(3) compareTo A(2) > 0) &&
    (A(2) compareTo A(2) == 0) &&
    (A(1) compareTo A(0) > 0) &&
    (A(3) compareTo A(4) < 0) &&
    (A(0) compareTo A(100) < 0)
