// WITH_RUNTIME

fun test(list: List<Int>) {
    val toSortedSet = list.<caret>filter { it > 1 }.toSortedSet()
}