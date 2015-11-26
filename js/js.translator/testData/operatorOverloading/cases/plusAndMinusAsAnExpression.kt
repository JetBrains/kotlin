package foo

class A() {

    operator fun unaryPlus() = "hooray"
    operator fun unaryMinus() = "not really"

}

fun box(): Boolean {
    var c = A()
    return (+c + -c == "hooraynot really")
}