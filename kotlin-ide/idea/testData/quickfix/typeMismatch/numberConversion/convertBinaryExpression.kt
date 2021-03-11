// "Convert expression to 'Float'" "true"
fun foo() {
    bar(1 + 3L<caret>)
}

fun bar(l: Float) {
}