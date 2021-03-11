// WITH_RUNTIME

fun foo() {
    val c: Collection<String> = listOf("")
    c.size<caret> > 0
}
