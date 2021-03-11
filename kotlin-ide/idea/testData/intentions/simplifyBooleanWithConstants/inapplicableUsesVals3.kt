// IS_APPLICABLE: false
fun foo(y: Boolean) {
    val x = true
    val z = false
    <caret>x && z 
}