package kotlin.js

// https://developer.mozilla.org/en/DOM/console
external public interface Console {
    public fun dir(o: Any): Unit = noImpl
    public fun error(vararg o: Any?): Unit = noImpl
    public fun info(vararg o: Any?): Unit = noImpl
    public fun log(vararg o: Any?): Unit = noImpl
    public fun warn(vararg o: Any?): Unit = noImpl
}

public external val console: Console = noImpl
