public class ClassWithNonPublicConstructor internal constructor(public val a: Int)

class Foo (a: Int) {
    constructor(f: Float) : this(f.toInt())

    private constructor(d: Double) : this(d.toInt())
    class INSIDE_CLASS {
        fun my_func(): Boolean = TODO()

        val my_value_inner: UInt = 5u

        var my_variable_inner: Long = 5
    }
    fun foo(): Boolean = TODO()

    val my_value: UInt = 5u

    var my_variable: Long = 5

}
