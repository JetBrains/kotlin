// IS_APPLICABLE: false
fun foo() {
    val x: Double<caret>
    val flag = false
    x = if (flag) 3.14 else 2.71
}