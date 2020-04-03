actual final class A1 actual constructor()
actual open class A2 actual constructor()
abstract class A3
sealed class A4

actual open class B1 actual constructor()
abstract class B2
sealed class B3

actual abstract class C1 actual constructor()
sealed class C2

actual sealed class D1

actual abstract class E actual constructor() {
    actual final val p1: Int = 1
    actual open val p2: Int = 1
    abstract val p3: Int

    actual open val p4: Int = 1
    abstract val p5: Int

    actual abstract val p6: Int

    actual final fun f1(): Int = 1
    actual open fun f2(): Int = 1
    abstract fun f3(): Int

    actual open fun f4(): Int = 1
    abstract fun f5(): Int

    actual abstract fun f6(): Int
}
