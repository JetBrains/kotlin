package namespace.deeper

/**
demo comment for packaged object
*/
object OBJECT_WITH_PACKAGE {
    class Foo
    class Bar(val i: Int) {
        fun bar(): Int = 5

        /**
         * demo comment for inner object
         */
        object OBJECT_INSIDE_CLASS
    }

    object OBJECT_INSIDE_OBJECT
    internal object INTERNAL_OBJECT_INSIDE_OBJECT

    fun foo(): Int = 5
    val value: Int = 5
    var variable: Int = 5
}

private object PRIVATE_OBJECT