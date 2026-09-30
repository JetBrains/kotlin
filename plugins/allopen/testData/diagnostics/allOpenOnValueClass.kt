// WITH_STDLIB
// LANGUAGE: +FullValueClasses
// FIR_DUMP

annotation class AllOpen

@AllOpen
value class <!VALUE_CLASS_OPEN!>Final<!>(val x: Int) {
    fun f() = x
}

@AllOpen
@JvmInline
value class <!VALUE_CLASS_NOT_FINAL!>Inline<!>(val x: Int)

@AllOpen
abstract value class Abstract {
    abstract val x: Int
    fun g() = x
}

value class <!VALUE_CLASS_OPEN!>Impl<!>(override val x: Int) : Abstract()

@AllOpen
sealed value class Sealed

value object Leaf : Sealed()

class Identity : Abstract() {
    override val x: Int get() = 0
}
