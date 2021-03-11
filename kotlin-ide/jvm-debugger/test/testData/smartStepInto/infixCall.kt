fun foo() {
    val a = A()
    f2(a f1 1)<caret>
}

class A {
    fun f1(i: Int) = 1
}

fun f2(i: Int) {}

// EXISTS: f1(Int), f2(Int)