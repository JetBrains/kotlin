class Test {
    internal var foo = 1

    init {
        foo = 2
        val foo = foo
    }
}
