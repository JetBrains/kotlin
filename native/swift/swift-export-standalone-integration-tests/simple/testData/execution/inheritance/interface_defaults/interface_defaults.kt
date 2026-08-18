// KIND: STANDALONE
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative
// MODULE: Inheritance
// FILE: interface_defaults.kt

// Defaulted interface methods (methods-only). A Swift class that inherits a Kotlin class and
// first-adopts this interface, without overriding `describe`, must inherit the Kotlin default —
// dispatched non-virtually so it never recurses through its patched itable slot. The default's open
// (virtual) self-call to the abstract `tag()` must still reach the Swift override.

open class Base {
    open fun greet(): String = "Hello from Kotlin"
}

interface Defaulter {
    fun tag(): String
    fun describe(): String = "default-describe(" + tag() + ")"
}

fun callDefDescribe(d: Defaulter): String = d.describe()
fun callDefTag(d: Defaulter): String = d.tag()
