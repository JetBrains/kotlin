// WITH_RUNTIME

fun test(i: Iterable<Int>): List<Int> {
    return i.<caret>filter { it > 1 }.map { it * 2 }
}