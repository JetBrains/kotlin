// IS_APPLICABLE: false
fun String.xxx(p: Int): Int = 0

fun foo(x: String) {
    x.xxx(<caret>1)
}