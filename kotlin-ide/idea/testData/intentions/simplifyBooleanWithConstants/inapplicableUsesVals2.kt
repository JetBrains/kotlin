// IS_APPLICABLE: false
fun foo(y: Boolean) {
    val x = 4
    val z = 5
    <caret>x == z || x != z
}