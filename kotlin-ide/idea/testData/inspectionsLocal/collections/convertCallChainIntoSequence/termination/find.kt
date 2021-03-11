// WITH_RUNTIME

fun test(list: List<Int>) {
    val find: Int? = list.<caret>filter { it > 1 }.find { true }
}