// PROBLEM: none
// WITH_RUNTIME

fun test(list: List<Int>) {
    list.<caret>groupingBy { it }.reduce { _, acc, _ -> acc }
}