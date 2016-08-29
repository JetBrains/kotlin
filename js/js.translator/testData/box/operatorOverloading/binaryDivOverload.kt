package foo

class A() {

    operator fun div(other: A) = "OK"

}

fun box() = A() / A()