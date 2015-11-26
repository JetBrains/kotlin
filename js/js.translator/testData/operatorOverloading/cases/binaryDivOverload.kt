package foo

class A() {

    operator fun div(other: A) = "hooray"

}

fun box() = ((A() / A()) == "hooray")