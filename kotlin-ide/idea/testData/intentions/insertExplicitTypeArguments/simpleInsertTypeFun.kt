// IS_APPLICABLE: true
fun foo() {
    <caret>bar("x")
}

fun <T> bar(t: T): Int = 1