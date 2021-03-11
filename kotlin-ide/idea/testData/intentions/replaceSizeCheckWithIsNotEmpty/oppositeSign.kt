// WITH_RUNTIME
// IS_APPLICABLE: false

fun foo() {
    val arrayOf = arrayOf(1, 2, 3)
    arrayOf.size<caret> < 0
}