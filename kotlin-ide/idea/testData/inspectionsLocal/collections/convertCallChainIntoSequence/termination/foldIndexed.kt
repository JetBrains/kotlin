// WITH_RUNTIME

fun test(list: List<Int>) {
    val foldIndexed = list.<caret>filter { it > 1 }.foldIndexed(0) { index, acc, i -> acc + i }
}