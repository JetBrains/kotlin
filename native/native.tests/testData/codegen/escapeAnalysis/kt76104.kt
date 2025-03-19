// IGNORE_NATIVE: optimizationMode=DEBUG
// IGNORE_NATIVE: optimizationMode=NO

// Enable runtime assertions:
// ASSERTIONS_MODE: always-enable

import kotlin.native.internal.*

class Foo(val value: Any)

fun box(): String {
    val any = Any()
    val list = mutableListOf<Foo>()
    if (!list.isStack()) return "FAIL"

    repeat(1) {
        list.add(Foo(any))
    }

    @OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
    kotlin.native.runtime.GC.collect()

    return "OK"
}