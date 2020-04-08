// WITH_RUNTIME

fun test(list: List<Int>) {
    val first: Int = list.<caret>filter { it > 1 }.first()
}