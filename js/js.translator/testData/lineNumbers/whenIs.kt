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

// LINES: 10 2 2 2 2 3 3 4 4 5 5 6 6 8 8 12 * 14 14