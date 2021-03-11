// WITH_RUNTIME

fun foo() {
    listOf(1).<caret>forEach {
        return@forEach
    }
}