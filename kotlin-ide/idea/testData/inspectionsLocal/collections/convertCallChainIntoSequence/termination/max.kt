// WITH_RUNTIME

fun test(list: List<Int>) {
    val max: Int? = list.<caret>filter { it > 1 }.max()
}