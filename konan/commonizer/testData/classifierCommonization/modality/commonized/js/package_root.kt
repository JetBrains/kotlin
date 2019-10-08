actual final class A1 actual constructor()
actual final class A2 actual constructor()
final class A3
final class A4

actual open class B1 actual constructor()
open class B2
open class B3

actual abstract class C1 actual constructor()
abstract class C2

actual sealed class D1

actual abstract class E actual constructor() {
    actual final val p1: Int = 1
    actual final val p2: Int = 1
    final val p3: Int = 1

    actual open val p4: Int = 1
    open val p5: Int = 1

    actual abstract val p6: Int

    actual final fun f1(): Int = 1
    actual final fun f2(): Int = 1
    final fun f3(): Int = 1

    actual open fun f4(): Int = 1
    open fun f5(): Int = 1

    actual abstract fun f6(): Int
}
