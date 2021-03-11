// PROBLEM: none
// WITH_RUNTIME

fun test(list: List<Int>) {
    val forEachIndexed: Unit = list.<caret>filter { it > 1 }.forEachIndexed { index, i -> }
}