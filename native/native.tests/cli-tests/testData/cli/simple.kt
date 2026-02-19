enum class E {
    A, B, C
}

private fun foo(e: E) {
    when (e) {
        E.A -> println("A")
        E.B -> println("B")
        E.C -> println("C")
    }
}

fun main() {
    foo(E.A)
}