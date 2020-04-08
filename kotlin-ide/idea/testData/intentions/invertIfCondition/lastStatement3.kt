fun foo(p: Int) {
    val x = 2
    if (p > 0) {
        <caret>if (x > 1) bar()
    }
}

fun bar(){}