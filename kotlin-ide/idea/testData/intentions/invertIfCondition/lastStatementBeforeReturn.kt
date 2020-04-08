fun foo(p: Boolean): Boolean {
    val x = 2
    <caret>if (x > 1) {
        bar()
    }
    return p
}

fun bar(){}
