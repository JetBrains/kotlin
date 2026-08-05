// KIND: STANDALONE
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative
// MODULE: Main
// FILE: suspend_error_type_fidelity.kt

// Not whether an error propagates, but whether it arrives as its *concrete* type: a Kotlin exception caught
// in Swift as the exported class, and a Swift value-type error round-tripping back through Kotlin as itself.

class AsyncException(message: String) : RuntimeException(message)

open class AsyncThrower {
    open suspend fun boom(): String {
        throw AsyncException("kotlin-boom")
    }
}

suspend fun callBoom(t: AsyncThrower): String = t.boom()
