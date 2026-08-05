// KIND: STANDALONE
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative
// MODULE: Inheritance
// FILE: error_non_throwable.kt

// A Kotlin type that is NOT a `Throwable` can still be thrown from Swift once it is retroactively conformed to
// `Swift.Error`. Kotlin cannot rethrow such an object as-is, so the reverse bridge has to box it into a
// `SwiftError` instead of handing its ref over as a throwable — which used to produce a ClassCastException.

class MyKotlinException(message: String) : RuntimeException(message)

// A plain Kotlin class that is NOT a `Throwable`: Swift may retroactively conform its exported class to
// `Swift.Error` and throw it from an override. Kotlin cannot rethrow a non-throwable, so it has to travel as a
// boxed `SwiftError` — and come back out to Swift as the very same instance.
class NotAThrowable(val tag: String)

// Open `@Throws` method with a non-Unit return and a parameter: a Swift override may throw a Swift
// error, which must surface to a Kotlin caller as a thrown exception (reverse error bridge).
open class Thrower {
    @Throws(Throwable::class)
    open fun mightThrow(prefix: String): String = prefix + "-kotlin-ok"
}

// Calls the (possibly Swift-overridden) method and reports what propagated back to Kotlin.
fun callMightThrowCatching(t: Thrower, prefix: String): String = try {
    "ok:" + t.mightThrow(prefix)
} catch (e: MyKotlinException) {
    "kotlin-exception:" + e.message
} catch (e: Throwable) {
    "throwable:" + (e.message ?: "?")
}

// Round-trip: a Swift override throws a Swift error; this `@Throws` relay propagates it back out to
// Swift, where it must arrive as the SAME Swift error (forward SwiftError unwrap).
open class Relayer {
    @Throws(Throwable::class)
    open fun relay(): String = "kotlin-relay"
}

@Throws(Throwable::class)
fun callRelay(r: Relayer): String = r.relay()
