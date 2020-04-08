class A<in I> {
    private val bar: I

    private fun foo(): I = null!!


    fun test() {
        with(A()) {
            this.<caret>
        }
    }
}

// INVOCATION_COUNT: 1
// ABSENT: bar, foo
