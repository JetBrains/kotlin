// WITH_RUNTIME

fun test(list: List<Int>) {
    val associateByTo = list.<caret>filter { it > 1 }.associateByTo(mutableMapOf()) { it }
}