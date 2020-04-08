package a

class A() {
}

operator fun A.component1() = 1
operator fun A.component2() = 2

<selection>fun f() {
    val (a, b) = A()
}</selection>