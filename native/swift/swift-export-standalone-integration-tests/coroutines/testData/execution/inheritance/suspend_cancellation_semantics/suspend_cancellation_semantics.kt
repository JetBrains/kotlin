// KIND: STANDALONE
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative
// MODULE: Main
// FILE: suspend_cancellation_semantics.kt

open class AsyncBase {
    open suspend fun greet(name: String): String = "Kotlin: $name"
}

suspend fun callGreet(base: AsyncBase, name: String): String = base.greet(name)
