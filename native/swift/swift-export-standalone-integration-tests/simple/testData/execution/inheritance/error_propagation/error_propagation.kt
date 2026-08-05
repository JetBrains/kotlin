// KIND: STANDALONE
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative
// MODULE: Inheritance
// FILE: error_propagation.kt


// --- Throwing reverse bridges ---

class MyKotlinException(message: String) : RuntimeException(message)

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

// A Kotlin super that throws: used to check that a Kotlin exception let-propagate by a Swift override
// (which called `super`) comes home to Kotlin as the ORIGINAL Kotlin exception (identity preserved).
open class SuperThrower {
    @Throws(Throwable::class)
    open fun boom(): String = throw MyKotlinException("kotlin-boom")
}

fun callBoomCatching(s: SuperThrower): String = try {
    s.boom()
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

// An exported Kotlin Throwable with an overridable member, so that a Swift subclass of it is a genuinely
// Swift-backed object (dynamic Kotlin class, patched slot) rather than a bare wrapper.
open class ThrowableBranch(val origin: String) : Exception("kotlin throwable: $origin") {
    open fun throwableValue(): String = "kotlin-throwable:$origin"
}

@Throws(Throwable::class)
fun throwProvided(value: ThrowableBranch): Unit = throw value
