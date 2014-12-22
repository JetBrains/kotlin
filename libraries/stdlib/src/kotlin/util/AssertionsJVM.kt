package kotlin


private object _Assertions

deprecated("Must be public to make assert() inlinable")
public val ASSERTIONS_ENABLED: Boolean = _Assertions.javaClass.desiredAssertionStatus()

/**
* Throws an [AssertionError] with an optional *message* if the *value* is false
* and runtime assertions have been enabled on the JVM using the *-ea* JVM option.
*/
public fun assert(value: Boolean, message: Any = "Assertion failed") {
    if (ASSERTIONS_ENABLED) {
        if (!value) {
            throw AssertionError(message)
        }
    }
}

/**
 * Throws an [AssertionError] calculated by *lazyMessage* if the *value* is false
 * and runtime assertions have been enabled on the JVM using the *-ea* JVM option.
 */
public inline fun assert(value: Boolean, lazyMessage: () -> Any) {
    if (ASSERTIONS_ENABLED) {
        if (!value) {
            val message = lazyMessage()
            throw AssertionError(message)
        }
    }
}
