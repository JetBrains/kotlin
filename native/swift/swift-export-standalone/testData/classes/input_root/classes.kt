/**
 * this is a sample comment for class without public constructor
 */
public class ClassWithNonPublicConstructor internal constructor(public val a: Int)

/**
 * this is a sample comment for class without package
 * in order to support documentation for primary constructor - we will have to start parsing comment content:
 * https://kotlinlang.org/docs/kotlin-doc.html#constructor
 */
class Foo (a: Int) {
    /**
     * this is a sample comment for secondary constructor
     */
    constructor(f: Float) : this(f.toInt())

    /**
     * this is a sample comment for private constructor
     */
    private constructor(d: Double) : this(d.toInt())

    /**
     * this is a sample comment for INSIDE_CLASS without package
     */
    class INSIDE_CLASS {
        /**
         * this is a sample comment for func on INSIDE_CLASS without package
         */
        fun my_func(): Boolean = TODO()

        /**
         * this is a sample comment for val on INSIDE_CLASS without package
         */
        val my_value_inner: UInt = 5u

        /**
         * this is a sample comment for var on INSIDE_CLASS without package
         */
        var my_variable_inner: Long = 5
    }
    /**
     * this is a sample comment for func on class without package
     */
    fun foo(): Boolean = TODO()

    /**
     * this is a sample comment for val on class without package
     */
    val my_value: UInt = 5u

    /**
     * this is a sample comment for var on class without package
     */
    var my_variable: Long = 5

    /**
     * should be ignored, as we did not designed this
     */
    companion object {
        fun COMPANION_OBJECT_FUNCTION_SHOULD_BE_IGNORED(): Int = TODO()
    }
}

class CLASS_WITH_SAME_NAME {
    fun foo(): Int = TODO()
}
