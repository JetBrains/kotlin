package kotlin

object Assertions {
    // TODO make private once KT-1528 is fixed.
    val _ENABLED = (javaClass<java.lang.System>()).desiredAssertionStatus()
}

/**
 * Throws an [[AssertionError]] if the `value` is false and runtime assertions have been
 * enabled on the JVM using the `-ea` JVM option.
 */
inline fun assert(value:Boolean):Unit {
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
inline fun assert<T>(value:Boolean, message:T) {
    if(Assertions._ENABLED) {
        if(!value) {
            throw AssertionError(message);
        }
    }
}

/**
 * Throws an [[IllegalArgumentException]] if the `value` is false.
 */
inline fun require(value:Boolean):Unit {
    if(!value) {
        throw IllegalArgumentException();
    }
}

/**
* Throws an [[IllegalArgumentException]] with specified `message` if the `value` is false.
*/
inline fun require<T>(value:Boolean, message:T) {
    if(!value) {
        throw IllegalArgumentException(message.toString());
    }
}
