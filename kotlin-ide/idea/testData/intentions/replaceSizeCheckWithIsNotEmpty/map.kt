// WITH_RUNTIME

fun foo() {
    val m = mapOf("" to 1)
    m.size<caret> > 0
}
