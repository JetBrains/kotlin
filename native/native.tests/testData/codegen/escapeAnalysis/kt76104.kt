// IGNORE_NATIVE: optimizationMode=DEBUG
// IGNORE_NATIVE: optimizationMode=NO
// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_FIRST_STAGE: Native:*
// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_STAGE: Native:*

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
