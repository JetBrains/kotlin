// KIND: STANDALONE
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative
// MODULE: Inheritance
// FILE: second_swift_level_interface.kt

// A Kotlin interface is first adopted by the *first* Swift class in the chain, and the object handed to Kotlin
// is an instance of the *second* Swift class, which inherits both the conformance and the implementation.
// Kotlin then dispatches through the interface, i.e. through an itable slot that only the Swift side populates:
//
//   itable slot -> Kotlin bridge -> @ImportedBridge -> @_cdecl ..._reverse_swift -> Swift override
//
// The reverse entry point rebuilds the receiver with `__createProtocolWrapper` / `__createClassWrapper` for the
// class that *declared* the conformance (level 1), while the object is an instance of level 2 — which is where
// the ClassCastException comes from.

open class InterfaceAnchor

interface SecondLevelContract {
    fun token(): String
}

fun callToken(value: SecondLevelContract): String = value.token()
