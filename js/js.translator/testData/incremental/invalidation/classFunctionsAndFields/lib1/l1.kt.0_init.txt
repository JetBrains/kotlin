
class Demo(val x: String) {
    fun foo() = "foo $x"
    inline fun foo_inline() = "inline foo $x"
    inline fun unused_inline() = "unused"

    val field1 = "field1"
}
