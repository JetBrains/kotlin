package a

class A() {}

operator fun A.plus(a: A) = this

infix fun A.infix(i: Int) = i

<selection>fun f(a: A) {
    a + a
    a infix 1
}</selection>