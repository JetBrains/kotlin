fun callFoos() {
    first.foo()
    second.foo()
}

fun use() {
    <caret>callFoos()
}
