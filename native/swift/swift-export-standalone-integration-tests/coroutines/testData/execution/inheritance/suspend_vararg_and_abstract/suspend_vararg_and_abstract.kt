// KIND: STANDALONE
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative
// MODULE: Main
// FILE: suspend_vararg_and_abstract.kt

open class AsyncVararg {
    open suspend fun join(vararg parts: String): String = "Kotlin: " + parts.joinToString(",")
}

suspend fun callJoin(v: AsyncVararg): String = v.join("a", "b", "c")
