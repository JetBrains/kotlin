package kotlin.js

// https://developer.mozilla.org/en/DOM/console
native public trait Console {
    native public fun dir(o: Any): Unit = noImpl
    native public fun error(vararg o: Any?): Unit = noImpl
    native public fun info(vararg o: Any?): Unit = noImpl
    native public fun log(vararg o: Any?): Unit = noImpl
    native public fun warn(vararg o: Any?): Unit = noImpl
}

native
public val console: Console = noImpl
