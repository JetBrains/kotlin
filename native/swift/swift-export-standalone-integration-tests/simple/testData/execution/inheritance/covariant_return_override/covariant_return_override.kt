// KIND: STANDALONE
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative
// MODULE: Inheritance
// FILE: covariant_return_override.kt

// A Swift override may narrow the return type of a Kotlin member.

open class ReturnBase {
    open fun tag(): String = "kotlin-base"
}

class ReturnDerived : ReturnBase() {
    override fun tag(): String = "kotlin-derived"
}

open class ReturnFactory {
    open fun make(): ReturnBase = ReturnBase()
}

// Calls the (possibly overridden) factory through the base static type and dispatches on the result.
fun callMakeTag(f: ReturnFactory): String = f.make().tag()

// Hands the produced object back to Swift as the base type, so the caller can check what it really is.
fun callMake(f: ReturnFactory): ReturnBase = f.make()
