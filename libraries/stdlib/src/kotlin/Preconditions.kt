package kotlin

object Assertions {
    // TODO make private once KT-1528 is fixed.
    val _ENABLED = (javaClass<java.lang.System>()).desiredAssertionStatus()
}

/**
 * Throws an [[AssertionError]] if the `value` is false and runtime assertions have been
 * enabled on the JVM using the `-ea` JVM option.
 */
inline fun assert(value: Boolean): Unit {
    if(Assertions._ENABLED) {
        if(!value) {
            throw AssertionError();
        }
    }
}

/**
* Throws an [[AssertionError]] with specified `message` if the `value` is false
* and runtime assertions have been enabled on the JVM using the `-ea` JVM option.
*/
inline fun assert(value: Boolean, message: Any) {
    if(Assertions._ENABLED) {
        if(!value) {
            throw AssertionError(message);
        }
    }
}

/**
* Throws an [[AssertionError]] with specified `message` if the `value` is false
* and runtime assertions have been enabled on the JVM using the `-ea` JVM option.
*/
inline fun assert(value: Boolean, lazyMessage: () -> String) {
    if(Assertions._ENABLED) {
        if(!value) {
            val message = lazyMessage()
            throw AssertionError(message);
        }
    }
}

/**
 * Throws an [[IllegalArgumentException]] if the `value` is false.
 */
inline fun require(value: Boolean): Unit {
    if(!value) {
        throw IllegalArgumentException();
    }
}

/**
 * Throws an [[IllegalArgumentException]] with specified `message` if the `value` is false.
 */
inline fun require(value: Boolean, message: Any): Unit {
    if(!value) {
        throw IllegalArgumentException(message.toString());
    }
}

/**
 * Throws an [[IllegalArgumentException]] with specified `message` if the `value` is false.
 */
inline fun require(value: Boolean, lazyMessage: () -> String): Unit {
    if(!value) {
        val message = lazyMessage()
        throw IllegalArgumentException(message.toString());
    }
}

/**
 * Throws an [[IllegalStateException]] if the `value` is false.
 */
inline fun check(value: Boolean): Unit {
    if(!value) {
        throw IllegalStateException();
    }
}

/**
 * Throws an [[IllegalStateException]] with specified `message` if the `value` is false.
 */
inline fun check(value: Boolean, message: Any): Unit {
    if(!value) {
        throw IllegalStateException(message.toString());
    }
}



/**
 * Throws an [[IllegalStateException]] with specified `message` if the `value` is false.
 */
inline fun check(value: Boolean, lazyMessage: () -> String): Unit {
    if(!value) {
        val message = lazyMessage()
        throw IllegalStateException(message.toString());
    }
}


