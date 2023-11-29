interface Foo {
    val someProperty: Int
    fun someMethod(): Any
    fun someMethodWithCovariantOverwrite(): Any
}

interface Bar : Foo {
    override fun someMethodWithCovariantOverwrite(): String = ""
}
