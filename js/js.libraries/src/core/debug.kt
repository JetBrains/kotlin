package js.debug

import js.noImpl

// https://developer.mozilla.org/en/DOM/console
native trait Console {
    native fun dir(o: Any): Unit = noImpl
    native fun error(vararg o: Any?): Unit = noImpl
    native fun info(vararg o: Any?): Unit = noImpl
    native fun log(vararg o: Any?): Unit = noImpl
    native fun warn(vararg o: Any?): Unit = noImpl
}

native
val console:Console = noImpl