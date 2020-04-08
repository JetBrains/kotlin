// WITH_RUNTIME

fun test() {
    Foo().apply {
        <caret>this.isB = true
    }
}
