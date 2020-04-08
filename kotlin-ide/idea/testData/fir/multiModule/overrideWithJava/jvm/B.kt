class B : A() {
    override fun foo(): B = this
    fun bar(): B = this // Here we should have "missing override" but no ambiguity

    fun test() {
        foo()
        bar()
    }
}
