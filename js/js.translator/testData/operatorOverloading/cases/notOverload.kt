package foo

class A() {

    operator fun not() = "hooray"

}

fun box() = (!A() == "hooray")