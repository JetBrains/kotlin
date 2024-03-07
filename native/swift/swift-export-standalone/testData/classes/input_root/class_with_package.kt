package namespace

class NAMESPACED_CLASS

class Foo {
    class INSIDE_CLASS

    fun foo(): Boolean = TODO()

    val my_value: UInt = 5u

    var my_variable: Long = 5

    fun createNamespacedClass(): NAMESPACED_CLASS = TODO()

    fun createDeeperNamespacedClass(): namespace.deeper.NAMESPACED_CLASS = TODO()
}
