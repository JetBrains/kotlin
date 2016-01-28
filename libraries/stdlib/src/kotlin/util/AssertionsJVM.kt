@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("PreconditionsKt")
package kotlin


private object _Assertions

internal val ASSERTIONS_ENABLED: Boolean = _Assertions.javaClass.desiredAssertionStatus()

/**
 * Throws an [AssertionError] if the [value] is false
 * and runtime assertions have been enabled on the JVM using the *-ea* JVM option.
 */
public fun assert(value: Boolean) {
    assert(value) { "Assertion failed" }
}

/**
 * Throws an [AssertionError] calculated by [lazyMessage] if the [value] is false
 * and runtime assertions have been enabled on the JVM using the *-ea* JVM option.
 */
@kotlin.internal.InlineOnly
public inline fun assert(value: Boolean, lazyMessage: () -> Any) {
    @Suppress("INVISIBLE_MEMBER_FROM_INLINE")
    if (ASSERTIONS_ENABLED) {
        if (!value) {
            val message = lazyMessage()
            throw AssertionError(message)
        }
    }
}
