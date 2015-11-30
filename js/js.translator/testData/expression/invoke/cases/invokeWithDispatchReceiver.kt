package foo

class A {
    operator fun invoke(i: Int) = i
}

fun box(): Boolean {
    return A()(1) == 1
}
