// WITH_RUNTIME

fun foo(value: Int?): Int? {
    return value<caret>?.let {
        println()
        it + 1
    }
}