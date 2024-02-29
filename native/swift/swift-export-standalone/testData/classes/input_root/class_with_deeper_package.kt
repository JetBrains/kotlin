package namespace.deeper

class NAMESPACED_CLASS

class Foo {
    class INSIDE_CLASS // this should be ignored currently

    fun foo(): Boolean = TODO()

    val my_value: UInt = 5u

    var my_variable: Long = 5

}
