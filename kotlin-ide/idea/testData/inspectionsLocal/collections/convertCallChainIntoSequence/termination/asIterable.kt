// WITH_RUNTIME

fun test(list: List<Int>) {
    val asIterable: Iterable<Int> = list.<caret>filter { it > 1 }.asIterable()
}