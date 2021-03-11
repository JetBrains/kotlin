// WITH_RUNTIME

fun test(list: List<Int>) {
    val firstOrNull: Int? = list.<caret>filter { it > 1 }.firstOrNull()
}