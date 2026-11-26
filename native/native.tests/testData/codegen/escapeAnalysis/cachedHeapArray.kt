// DISABLE_NATIVE: optimizationMode=OPT && cacheMode=STATIC_PER_FILE_EVERYWHERE
// DISABLE_NATIVE: optimizationMode=OPT && cacheMode=STATIC_USE_HEADERS_EVERYWHERE

// MODULE: lib
// FILE: lib.kt

fun storeInHeapArray(value: Any, size: Int): Any {
    // The size is unknown when compiling this function into a cache, so the array must be heap allocated.
    val array = arrayOfNulls<Any>(size)
    array[0] = value
    return array[0]!!
}

// MODULE: main(lib)
// FILE: main.kt

import kotlin.native.internal.isStack
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class Payload(val value: Int)

fun box(): String {
    val payload = Payload(42)
    val onStack = payload.isStack()
    // Also exceed the stack array allocation budget when the library is compiled without caches and inlined.
    val result = storeInHeapArray(payload, 100_000) as Payload
    assertEquals(42, result.value)
    assertFalse(onStack, "An object stored in a cached function's heap array must escape")
    return "OK"
}
