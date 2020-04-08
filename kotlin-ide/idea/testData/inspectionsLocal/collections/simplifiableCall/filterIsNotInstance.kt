// WITH_RUNTIME
// PROBLEM: none
fun test(list: List<Any>) {
    list.<caret>filter { it !is String }
}