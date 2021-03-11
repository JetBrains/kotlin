// WITH_RUNTIME

fun test(list: List<Int>) {
    val sum: Int = list.<caret>filter { it > 1 }.sum()
}