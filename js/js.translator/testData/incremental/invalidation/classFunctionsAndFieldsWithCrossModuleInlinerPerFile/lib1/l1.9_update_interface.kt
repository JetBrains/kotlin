interface DemoInterface {
    fun foo(): Any
}

class Demo(val x: String, val y: String = "default") : DemoInterface {
    override fun foo() = "foo $x update"
    inline fun foo_inline() = "inline foo $x update"
    inline fun unused_inline() = "unused update"

    val field1 = "field1 update"
    val field2 = "field2"
}
