// PROBLEM: none
// WITH_RUNTIME

fun main() {
    listOf(42).map {
        it.let {
            <caret>it == 42
        }
    }
}