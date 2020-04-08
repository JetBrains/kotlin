fun foo() {
    val x = true
    val y = false
    (((((x || false)) && y)) <caret>|| false)
}