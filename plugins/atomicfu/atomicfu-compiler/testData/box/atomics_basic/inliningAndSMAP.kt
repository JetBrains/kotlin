// DUMP_SMAP

import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic

class TestClass {
    private val atomicRefProp: AtomicRef<String> = atomic<String>("OK")

    fun foo(): String {
        return atomicRefProp.inlineBar()
    }
}

internal inline fun AtomicRef<String>.inlineBar(): String {
    return this.value
}

fun box(): String {
    return TestClass().foo()
}