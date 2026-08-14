// KIND: STANDALONE
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative
// MODULE: Main
// FILE: suspend_overloads.kt

// A suspend function additionally gains a continuation parameter in its lowered signature
open class AsyncOverloads {
    open suspend fun pick(arg1: String): String = "kotlin-pick($arg1)"
    open suspend fun pick(arg1: String, arg2: Int): String = "kotlin-pick($arg1, $arg2)"
    open suspend fun same(arg: String): String = "kotlin-same-string($arg)"
    open suspend fun same(arg: Int): String = "kotlin-same-int($arg)"
}

// Each driver calls exactly one overload, so an assertion pins down which slot was actually patched.
suspend fun callPick1(o: AsyncOverloads, arg1: String): String = o.pick(arg1)
suspend fun callPick2(o: AsyncOverloads, arg1: String, arg2: Int): String = o.pick(arg1, arg2)
suspend fun callSameString(o: AsyncOverloads, arg: String): String = o.same(arg)
suspend fun callSameInt(o: AsyncOverloads, arg: Int): String = o.same(arg)
