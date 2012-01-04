package foo

class A() {

    fun not() = "hooray"

}

fun box() = (!A() == "hooray")