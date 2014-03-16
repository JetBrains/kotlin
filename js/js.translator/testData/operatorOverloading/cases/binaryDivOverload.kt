package foo

class A() {

    fun div(other: A) = "hooray"

}

fun box() = ((A() / A()) == "hooray")