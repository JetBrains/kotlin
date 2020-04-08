// WITH_RUNTIME

fun test(list: List<Int>) {
    val average: Double = list.<caret>filter { it > 1 }.average()
}