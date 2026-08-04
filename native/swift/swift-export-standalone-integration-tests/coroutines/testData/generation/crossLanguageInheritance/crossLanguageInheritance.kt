// KIND: STANDALONE
// APPLE_ONLY_VALIDATION
// MODULE: main
// FILE: main.kt

open class AsyncBase {
    open suspend fun greet(name: String): String = "Hello, $name"
    open suspend fun count(): Int = 42
    suspend fun notOpen(): String = "final"
    open fun sync(name: String): String = "Hi, $name"
}

abstract class AsyncAbstractBase {
    abstract suspend fun abstractGreet(): String
    open suspend fun concreteGreet(): String = "concrete"
}

interface AsyncGreeter {
    suspend fun greet(name: String): String
    suspend fun salutation(): String
}

open class AsyncGreeterBase : AsyncGreeter {
    override suspend fun greet(name: String): String = "Hello, $name"
    override suspend fun salutation(): String = "Hi"
}

// Defaulted suspend interface method: `describe` (default, gets a non-virtual `_direct` async witness
// in the unconstrained extension) vs `tag` (abstract, no witness — Swift conformers must implement it).
interface AsyncDefaulter {
    suspend fun tag(): String
    suspend fun describe(): String = "default: ${tag()}"
}

// Overloaded suspend members: the reverse bridge of every overload must take over the virtual table slot
// of that exact overload. `overloaded()` is final, so it has no slot at all, and the `same` overloads are
// told apart by their parameter types only. Suspend signatures gain a continuation parameter on the way
// to the virtual table, so this also covers matching lowered signatures.
open class AsyncOverloaded {
    suspend fun overloaded(): String = "final"
    open suspend fun overloaded(arg1: String): String = "overloaded($arg1)"
    open suspend fun overloaded(arg1: String, arg2: Int): String = "overloaded($arg1, $arg2)"
    open suspend fun same(arg: String): String = "same(String)"
    open suspend fun same(arg: Int): String = "same(Int)"
}
