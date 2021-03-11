package a

private fun <caret>foo() {
    bar()
}

private fun bar() {
    foo()
}