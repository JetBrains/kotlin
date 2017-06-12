// EXPECTED_REACHABLE_NODES: 499
package foo

open class A(var a: Int) {

    open fun Int.modify(): Int {
        return this * 3;
    }

    fun eval() = a.modify();
}

class B(a: Int) : A(a) {
    override fun Int.modify(): Int {
        return this - 2;
    }
}

fun box(): String {
    return if ((A(4).eval() == 12) && (A(2).eval() == 6) && (B(3).eval() == 1)) "OK" else "fail"
}
