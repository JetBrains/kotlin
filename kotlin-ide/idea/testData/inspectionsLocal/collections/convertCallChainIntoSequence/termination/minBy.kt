// WITH_RUNTIME

fun test(list: List<Int>) {
    val minBy: Int? = list.<caret>filter { it > 1 }.minBy { true }
}