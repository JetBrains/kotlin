private class A {
    val bar = 1
    fun <caret>foooo() {
        { this.bar }()
        bar
    }
}

private fun test(a: A) {
    a.foooo()
}