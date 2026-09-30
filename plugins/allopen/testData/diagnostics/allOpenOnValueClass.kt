// ISSUE: KT-89973
// WITH_STDLIB
// LANGUAGE: +FullValueClasses
// FIR_DUMP

annotation class AllOpen

@AllOpen
value class Final(val x: Int) {
    fun f() = x
}

@AllOpen
@JvmInline
value class Inline(val x: Int)

@AllOpen
abstract value class Abstract {
    abstract val x: Int
    fun g() = x
}

value class Impl(override val x: Int) : Abstract()

@AllOpen
sealed value class Sealed

value object Leaf : Sealed()

class Identity : Abstract() {
    override val x: Int get() = 0
}
