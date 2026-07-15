// DISABLE_NATIVE: optimizationMode=DEBUG
// DISABLE_NATIVE: optimizationMode=NO
// DISABLE_NATIVE: cacheMode=STATIC_ONLY_DIST
// DISABLE_NATIVE: cacheMode=STATIC_EVERYWHERE
// DISABLE_NATIVE: cacheMode=STATIC_PER_FILE_EVERYWHERE
// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_FIRST_STAGE: Native:*
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
