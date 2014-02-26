package foo

class A(var a: Int) {
    fun eval() = f();
}

fun A.f(): Int {
    a = 3
    return 10
}

fun box(): Boolean {
    val a = A(4)
    return (a.eval() == 10) && (a.a == 3)
}
