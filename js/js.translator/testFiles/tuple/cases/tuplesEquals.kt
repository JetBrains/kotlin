package foo

class A(val i: Int = 2) {
    fun equals(other: Any?): Boolean {
        if (other !is A) {
            return false
        }
        return i % 2 == other.i % 2
    }
}

fun box(): Boolean {
    val c = #(3, 1)
    if (c != #(3, 1)) {
        return false
    }
    if (c == #(1, 3)) {
        return false
    }
    val b = #(A(2), A(1))
    if (b != #(A(0), A(3))) {
        return false
    }
    if (b == #(A(2), A(4))) {
        return false
    }
    if (#("aaa") != #("aaa")) {
        return false
    }
    return true
}
