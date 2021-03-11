fun foo() {
    for (i in 1..10) {
        <caret>if (i > 1) {
            bar()
        }
    }
}

fun bar(){}