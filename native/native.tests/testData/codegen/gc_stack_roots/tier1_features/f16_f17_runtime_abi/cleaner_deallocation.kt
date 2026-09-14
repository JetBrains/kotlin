// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class, kotlin.experimental.ExperimentalNativeApi::class)
package gc_stack_roots.tier1_features.f16_f17_runtime_abi

import kotlin.native.runtime.GC
import kotlin.native.ref.createCleaner
import kotlin.native.ref.Cleaner
import kotlin.concurrent.AtomicInt

class ObservedObject(val id: Int)

val cleanedCount = AtomicInt(0)

fun createWithCleaner(): Cleaner {
    val obj = ObservedObject(42)
    return createCleaner(obj) {
        cleanedCount.incrementAndGet()
    }
}

fun box(): String {
    val cleaner = createWithCleaner()

    GC.collect()

    if (cleanedCount.value != 1) {
        return "FAIL: cleaner was not executed: ${cleanedCount.value}"
    }
    return "OK"
}
