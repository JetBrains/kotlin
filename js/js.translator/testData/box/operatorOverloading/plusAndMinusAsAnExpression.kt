package foo

class A() {

    operator fun unaryPlus() = "O"
    operator fun unaryMinus() = "K"

}

fun box(): String {
    var c = A()
    return +c + -c
}