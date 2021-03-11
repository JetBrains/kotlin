fun foo() {
    val x = 2
    <caret>if (x <= 1) return
    bar()
}

fun bar(){}
