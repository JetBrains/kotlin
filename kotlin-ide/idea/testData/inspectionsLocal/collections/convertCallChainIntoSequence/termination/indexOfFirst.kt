// WITH_RUNTIME

fun test(list: List<Int>) {
    val indexOfFirst: Int = list.<caret>filter { it > 1 }.indexOfFirst { true }
}