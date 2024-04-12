/**
    demo comment for packageless object
*/
object OBJECT_NO_PACKAGE {
    class Foo
    class Bar(val i: Int) {
        fun bar(): Int = 5
        class CLASS_INSIDE_CLASS_INSIDE_OBJECT

        /**
         * we do not support companion objects currently.
         * https://youtrack.jetbrains.com/issue/KT-66817
         */
        companion object {
            fun foo(): Int = TODO()
        }
    }

    object OBJECT_INSIDE_OBJECT
    internal object INTERNAL_OBJECT_INSIDE_OBJECT

    fun foo(): Int = 5
    val value: Int = 5
    var variable: Int = 5
}

private object PRIVATE_OBJECT