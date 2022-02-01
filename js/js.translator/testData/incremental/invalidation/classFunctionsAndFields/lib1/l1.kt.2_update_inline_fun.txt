
class Demo(val x: String) {
    fun foo() = "foo $x update"
    inline fun foo_inline() = "inline foo $x update"
    inline fun unused_inline() = "unused"

    val field1 = "field1"
}
