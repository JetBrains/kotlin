package a

class A {
}

fun A.ext() {
}

infix fun A.infix(a: A) {
}

operator fun A.plus(a: A) {
}

operator fun A.unaryMinus() {
}

val A.p: Int
    get() = 2

<selection>fun f() {
    A().ext()
    A() + A()
    A() infix A()
    -A()
    A().p
}</selection>