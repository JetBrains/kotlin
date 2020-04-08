// ERROR: A 'return' expression required in a function with a block body ('{...}')

fun foo(): Int {
    val x = 2
    <caret>if (x > 1) {
        bar()
    }
}

fun bar(){}