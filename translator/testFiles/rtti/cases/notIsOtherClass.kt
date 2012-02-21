package foo

open class A() {

}

class B() : A() {

}

fun box() = (A() !is B)