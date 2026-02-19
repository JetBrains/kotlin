package foo

class A() {

    operator fun not() = "OK"

}

fun box() = !A()