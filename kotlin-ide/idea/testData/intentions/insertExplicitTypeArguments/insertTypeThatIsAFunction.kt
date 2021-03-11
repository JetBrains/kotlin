// IS_APPLICABLE: true
fun foo() {
    <caret>bar({ i: Int -> 2 * i })
}

fun <T> bar(t: T): Int = 1