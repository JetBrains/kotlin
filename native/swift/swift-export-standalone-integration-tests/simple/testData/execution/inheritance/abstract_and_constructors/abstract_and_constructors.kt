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

// A Swift class inheriting an abstract Kotlin class must be able to call `super.init`, which runs the
// abstract class's constructor to initialize its state (`prefix`), and override its abstract members.
// The inherited concrete `decorated()` combines the ctor-initialized state with a virtual self-call to
// the abstract `greeting()`, which must reach the Swift override. Direct instantiation of the abstract
// class is forbidden at runtime by a precondition in the generated initializer.
abstract class AbstractGreeter {
    val prefix: String = "kotlin-prefix"
    abstract fun greeting(): String
    open fun decorated(): String = prefix + ":" + greeting()
}

fun callGreeting(g: AbstractGreeter): String = g.greeting()
fun callDecorated(g: AbstractGreeter): String = g.decorated()

// Abstract class with a constructor parameter: a Swift subclass must pass it through `super.init`, and
// the inherited concrete `total()` must combine the ctor-initialized `start` with the Swift override of
// the abstract `step()`.
abstract class AbstractCounter(val start: Int) {
    abstract fun step(): Int
    open fun total(): Int = start + step()
}

fun callTotal(c: AbstractCounter): Int = c.total()
