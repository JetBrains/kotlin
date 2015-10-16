package foo

class A

fun box(): Boolean {
    val a = A()
    val b = fun A.(i: Int) = i
    return a.(b)(1) == 1
}
