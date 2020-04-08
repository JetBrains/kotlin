// WITH_RUNTIME

fun test(list: List<Int>) {
    val indexOfLast: Int = list.<caret>filter { it > 1 }.indexOfLast { true }
}