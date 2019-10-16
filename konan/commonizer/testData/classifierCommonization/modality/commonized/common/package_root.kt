expect final class A1()
expect final class A2()
expect open class B1()
expect abstract class C1()
expect sealed class D1

expect abstract class E() {
    final val p1: Int
    final val p2: Int
    open val p4: Int
    abstract val p6: Int

    final fun f1(): Int
    final fun f2(): Int
    open fun f4(): Int
    abstract fun f6(): Int
}
