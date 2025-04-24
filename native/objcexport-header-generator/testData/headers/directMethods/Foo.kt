open class Foo {
    fun finalNonOveriding() {}
    override fun toString() = "Foo"
    open fun open(): Foo = Foo()
}

data class Bar(val a: Int, val b: String)
