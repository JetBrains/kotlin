// IS_APPLICABLE: false
fun foo() {
    val x = <caret>bar<String>("x")
}

fun <T> bar(t: T): Int = 1