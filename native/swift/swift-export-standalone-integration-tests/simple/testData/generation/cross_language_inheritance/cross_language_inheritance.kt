// KIND: STANDALONE
// MODULE: main
// FILE: main.kt

open class Base {
    open fun greet(name: String): String = "Hello, $name"
    open fun count(): Int = 42
    fun notOpen(): String = "final"
    // Open class properties: get-only (`val`) and settable (`var`). Each accessor gets a reverse
    // bridge plus a non-virtual `_direct` forward bridge, and a branched body.
    open val size: Int = 0
    open var name: String = ""
    // Final property: no reverse/`_direct` bridges (mirrors `notOpen`).
    val notOpenValue: String = "final"
}

abstract class AbstractBase {
    abstract fun abstractMethod(): String
    open fun concreteMethod(): Int = 0
}

interface Greeter {
    fun greet(name: String): String
    fun salutation(): String
    // Abstract interface property (settable): reverse bridges use the protocol existential as self;
    // no `_direct` bridge and no witness (abstract — Swift conformers must implement it).
    var mood: String
}

open class GreeterBase : Greeter {
    override fun greet(name: String): String = "Hello, $name"
    override fun salutation(): String = "Hi"
    override var mood: String = "ok"
}

// Defaulted interface methods: `describe` (default, gets a non-virtual `_direct` witness in the
// unconstrained extension) vs `tag` (abstract, no witness — Swift conformers must implement it).
interface Defaulter {
    fun tag(): String
    fun describe(): String = "default: ${tag()}"
    // Defaulted interface property (`val` with a default getter): gets a non-virtual `_direct`
    // witness in the unconstrained extension so a Swift conformer inherits the default.
    val kind: String get() = "kind: ${tag()}"
}

// Generic interface with a defaulted method whose signature does not mention the type parameter:
// exercises PARAMETRIZED receiver rendering in the `_direct` bridge for a generic interface.
interface Boxed<T> {
    fun unbox(): T
    fun label(): String = "boxed"
    // Defaulted property on a generic interface: exercises PARAMETRIZED receiver rendering in the
    // property `_direct` witness.
    val boxLabel: String get() = "boxLabel"
}

// A throwing (`@Throws`) open method with a non-Unit return: its reverse bridge carries a Swift-thrown
// error back to Kotlin through an out-error slot (the mirror of the forward `_out_error` channel), and
// the non-Unit return exercises the scalar catch-path default.
open class ThrowingMembers {
    @Throws(Throwable::class)
    open fun compute(x: Int): String = "computed: $x"
}

// Overloaded members: the reverse bridge of every overload must take over the virtual table slot of
// that exact overload. `pick()` is final, so it has no slot at all; the `same` overloads differ only
// in parameter types and the `nullable` ones only in their nullability.
open class Overloaded {
    fun pick(): String = "final"
    open fun pick(arg1: String): String = "pick($arg1)"
    open fun pick(arg1: String, arg2: Int): String = "pick($arg1, $arg2)"
    open fun same(arg: String): String = "same(String)"
    open fun same(arg: Int): String = "same(Int)"
    open fun nullable(arg: String): String = "nullable(String)"
    open fun nullable(arg: String?): String = "nullable(String?)"
}

// The same, but on an interface: the reverse bridges are placed into the interface table.
interface OverloadedInterface {
    fun say(): String
    fun say(times: Int): String
}
