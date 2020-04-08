// WITH_RUNTIME

fun test(s: String?): Int? {
    return s?.let<caret> {
        it.length
    }
}