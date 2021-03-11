// PROBLEM: none
// WITH_RUNTIME

enum class E {
    A, B
}

fun main() {
    val map = <caret>hashMapOf<E, String>()
}
