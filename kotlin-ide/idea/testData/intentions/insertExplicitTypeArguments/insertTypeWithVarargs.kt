// IS_APPLICABLE: true
fun foo() {
    <caret>bar(1, 2, 3, 4)
}

fun <T> bar(vararg ts: T): Int = 1