// IGNORE_NATIVE: optimizationMode=DEBUG
// IGNORE_NATIVE: optimizationMode=NO

// Enable runtime assertions:
// ASSERTIONS_MODE: always-enable

import kotlin.native.internal.*

class Foo

fun box(): String {
    val list = mutableListOf<Foo>()
    if (!list.isStack()) return "FAIL"

    list.add(Foo())

    @OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
    kotlin.native.runtime.GC.collect()

    return "OK"
}