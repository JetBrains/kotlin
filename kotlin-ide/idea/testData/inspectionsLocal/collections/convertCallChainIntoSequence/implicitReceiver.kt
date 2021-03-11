// WITH_RUNTIME

fun List<Int>.test(): List<Int> {
    return <caret>filter { it > 1 }.map { it * 2 }
}