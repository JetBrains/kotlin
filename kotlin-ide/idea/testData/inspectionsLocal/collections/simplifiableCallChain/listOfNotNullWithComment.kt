// WITH_RUNTIME

fun test() {
    <caret>listOf(
            true, // comment1
            null // comment2
    ).filterNotNull().first()
}