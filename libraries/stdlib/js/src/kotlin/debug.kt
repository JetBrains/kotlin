package kotlin.js

/**
 * Exposes the [console API](https://developer.mozilla.org/en/DOM/console) to Kotlin.
 */
external public interface Console {
    public fun dir(o: Any): Unit
    public fun error(vararg o: Any?): Unit
    public fun info(vararg o: Any?): Unit
    public fun log(vararg o: Any?): Unit
    public fun warn(vararg o: Any?): Unit
}

/**
 * Exposes the [console API](https://developer.mozilla.org/en/DOM/console) to Kotlin.
 */
public external val console: Console
