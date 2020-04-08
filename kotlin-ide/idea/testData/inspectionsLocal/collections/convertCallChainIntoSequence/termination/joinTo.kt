// WITH_RUNTIME

fun test(list: List<Int>) {
    val joinTo: StringBuilder = list.<caret>filter { it > 1 }.joinTo(StringBuilder())
}