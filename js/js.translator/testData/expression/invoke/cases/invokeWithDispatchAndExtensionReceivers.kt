package foo

class A
class B {
    fun A.invoke(i: Int) = i
}

fun box(): Boolean {
    val a = A()
    val b = B()
    return a.(b)(1) == 1
}
