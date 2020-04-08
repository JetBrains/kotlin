// IS_APPLICABLE: true
fun <T> foo(t: T) {
    <caret>bar(t)
}

fun <T> bar(t: T): Int = 1