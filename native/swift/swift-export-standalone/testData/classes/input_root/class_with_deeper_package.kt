package namespace.deeper

class NAMESPACED_CLASS

class Foo {
    class INSIDE_CLASS {
        class DEEPER_INSIDE_CLASS {
            fun foo(): Boolean = TODO()

            val my_value: UInt = 5u

            var my_variable: Long = 5
        }

        fun foo(): Boolean = TODO()

        val my_value: UInt = 5u

        var my_variable: Long = 5
    }

    fun foo(): Boolean = TODO()

    val my_value: UInt = 5u

    var my_variable: Long = 5

}
