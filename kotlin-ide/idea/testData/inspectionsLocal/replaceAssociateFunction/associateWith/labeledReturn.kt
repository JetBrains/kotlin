// PROBLEM: none
// WITH_RUNTIME
fun getValue(i: Int): String = ""

fun test(b: Boolean) {
    listOf(1).<caret>associate {
        if (b) {
            return@associate it to ""
        }
        it to getValue(it)
    }
}