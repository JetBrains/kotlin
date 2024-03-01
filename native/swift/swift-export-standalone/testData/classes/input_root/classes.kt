
class Foo {
    class INSIDE_CLASS {
        fun my_func(): Boolean = TODO()

        val my_value_inner: UInt = 5u

        var my_variable_inner: Long = 5
    }
    fun foo(): Boolean = TODO()

    val my_value: UInt = 5u

    var my_variable: Long = 5

}
