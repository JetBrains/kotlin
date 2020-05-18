// WITH_RUNTIME

fun test(): List<Int> {
    return setOf(1).<caret>filterNotNull()
}
