// IS_APPLICABLE: false
fun foo() {
    val x = bar("x") { <caret>2 }
}

fun <T> bar(t: T, v : () -> Int): Int = 1