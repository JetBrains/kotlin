fun box() {
    when (A()) {
        is A ->
            println("A")
        is B ->
            println("B")
        else ->
            println("other")
    }
}

open class A

open class B : A()

// LINES: 2 2 3 4 5 6 8 * 14