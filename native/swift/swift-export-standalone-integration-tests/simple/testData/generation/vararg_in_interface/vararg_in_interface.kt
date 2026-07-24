// KIND: STANDALONE
// MODULE: main
// FILE: main.kt

interface Driver {
    fun addListener(vararg queryKeys: String, listener: Listener)

    interface Listener
}

open class BaseDriver {
    open fun addListener(vararg queryKeys: String) {}
    open fun addInts(vararg queryKeys: Int) {}
    open fun addOptionalInts(vararg queryKeys: Int?) {}
}
