// EXPECTED_REACHABLE_NODES: 492
package foo

class A(var a: Int) {
    fun eval() = f();
}

fun A.f(): Int {
    a = 3
    return 10
}

fun box(): String {
    val a = A(4)
    return if ((a.eval() == 10) && (a.a == 3)) "OK" else "fail"
}
