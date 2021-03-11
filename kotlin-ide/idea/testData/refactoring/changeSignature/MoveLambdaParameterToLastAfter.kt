fun <caret>foo(y: Int, x: () -> Unit) {
}

fun test() {
    foo(1) { 2 }
}