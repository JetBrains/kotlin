interface Foo {
    val someProperty: Int
    fun someMethod(): Any
    fun someMethodWithCovariantOverwrite(): Any
}

class Bar : Foo {
    override val someProperty: Int = 42
    override fun someMethod(): Any = ""
    override fun someMethodWithCovariantOverwrite(): String = ""
}