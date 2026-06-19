// KIND: STANDALONE
// MODULE: main
// FILE: main.kt

open class Base {
    open fun greet(name: String): String = "Hello, $name"
    open fun count(): Int = 42
    fun notOpen(): String = "final"
}

abstract class AbstractBase {
    abstract fun abstractMethod(): String
    open fun concreteMethod(): Int = 0
}

interface Greeter {
    fun greet(name: String): String
    fun salutation(): String
}

open class GreeterBase : Greeter {
    override fun greet(name: String): String = "Hello, $name"
    override fun salutation(): String = "Hi"
}

// Defaulted interface methods: `describe` (default, gets a non-virtual `_direct` witness in the
// unconstrained extension) vs `tag` (abstract, no witness — Swift conformers must implement it).
interface Defaulter {
    fun tag(): String
    fun describe(): String = "default: ${tag()}"
}

// Generic interface with a defaulted method whose signature does not mention the type parameter:
// exercises PARAMETRIZED receiver rendering in the `_direct` bridge for a generic interface.
interface Boxed<T> {
    fun unbox(): T
    fun label(): String = "boxed"
}
