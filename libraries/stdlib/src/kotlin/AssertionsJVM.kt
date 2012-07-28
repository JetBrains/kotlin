package kotlin

object Assertions {
    // TODO make private once KT-1528 is fixed.
    val _ENABLED = (javaClass<java.lang.System>()).desiredAssertionStatus()
}

/**
* Throws an [[AssertionError]] with an optional *message* if the *value* is false
* and runtime assertions have been enabled on the JVM using the *-ea* JVM option.
*/
public inline fun assert(value: Boolean, message: Any = "Assertion failed") {
    if (Assertions._ENABLED) {
        if (!value) {
            throw AssertionError(message)
        }
    }
}

/**
 * Throws an [[AssertionError]] with the specified *lazyMessage* if the *value* is false
 * and runtime assertions have been enabled on the JVM using the *-ea* JVM option.
 */
public inline fun assert(value: Boolean, lazyMessage: () -> String) {
    if (Assertions._ENABLED) {
        if (!value) {
            val message = lazyMessage()
            throw AssertionError(message)
        }
    }
}
