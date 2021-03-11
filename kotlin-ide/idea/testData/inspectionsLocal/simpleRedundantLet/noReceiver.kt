// WITH_RUNTIME

fun String.test(): Int {
    return let<caret> {
        it.length
    }
}