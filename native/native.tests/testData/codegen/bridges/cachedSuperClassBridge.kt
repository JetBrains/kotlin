// NATIVE_STANDALONE
// DISABLE_NATIVE: optimizationMode=OPT && cacheMode=STATIC_PER_FILE_EVERYWHERE
// DISABLE_NATIVE: optimizationMode=OPT && cacheMode=STATIC_USE_HEADERS_EVERYWHERE

// MODULE: lib
// FILE: lib.kt

open class Base {
    fun value(): Int = 42
}

// MODULE: main(lib)
// FILE: main.kt

import kotlin.test.assertEquals

interface Value<T> {
    fun value(): T
}

// The bridge is generated in this module, but its target belongs to the cached library.
class Derived : Base(), Value<Int>

private fun consume(value: Value<Int>): Int = value.value()

fun box(): String {
    assertEquals(42, consume(Derived()))
    return "OK"
}
