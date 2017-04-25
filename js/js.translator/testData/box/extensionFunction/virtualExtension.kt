// EXPECTED_REACHABLE_NODES: 492
package foo

class A(var a: Int) {

    fun Int.modify(): Int {
        return this * 3;
    }

    fun eval() = a.modify();
}

fun box(): String {
    val a = A(4)
    return if (a.eval() == 12) "OK" else "fail"
}
