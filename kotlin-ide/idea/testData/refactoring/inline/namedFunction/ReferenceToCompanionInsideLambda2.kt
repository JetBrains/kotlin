package abc

private class A {
    fun <caret>foooo(): Int {
        return { bar }()
    }

    companion object {
        const val bar = 4
    }
}

private fun test(a: A) {
    val bar = 42
    val c = a.foooo()
}