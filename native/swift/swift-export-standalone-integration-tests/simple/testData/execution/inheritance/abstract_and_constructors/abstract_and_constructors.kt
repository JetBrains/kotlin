// KIND: STANDALONE
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative
// MODULE: Inheritance
// FILE: abstract_and_constructors.kt

// Swift inheritance through abstract Kotlin ancestry.

abstract class AbstractRoot {
    abstract fun abstractValue(): String
    open fun concreteValue(): String = "abstract-concrete"
}

open class ConcreteAbstractBranch : AbstractRoot() {
    override open fun abstractValue(): String = "kotlin-abstract"
}

fun callAbstractValue(value: AbstractRoot): String = value.abstractValue()
fun callAbstractConcrete(value: AbstractRoot): String = value.concreteValue()

// Primary, secondary and default-argument constructor ancestry.

open class ConstructorBase(val constructorOrigin: String = "primary-default") {
    constructor(number: Int) : this("secondary:$number")

    open fun constructorValue(): String = constructorOrigin
}

// Kotlin default arguments are not emitted as Swift default parameters. This branch consumes the
// default on the Kotlin side and exposes a no-argument class that Swift can extend.
open class DefaultConstructorBranch : ConstructorBase()

fun callConstructorValue(value: ConstructorBase): String = value.constructorValue()
