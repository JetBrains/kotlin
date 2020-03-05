final class A1
final class A2
final class A3
final class A4

open class B1
open class B2
open class B3

abstract class C1
abstract class C2

sealed class D1

abstract class E {
    final val p1: Int = 1
    final val p2: Int = 1
    final val p3: Int = 1

    open val p4: Int = 1
    open val p5: Int = 1

    abstract val p6: Int

    final fun f1(): Int = 1
    final fun f2(): Int = 1
    final fun f3(): Int = 1

    open fun f4(): Int = 1
    open fun f5(): Int = 1

    abstract fun f6(): Int
}
