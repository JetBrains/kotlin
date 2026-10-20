// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier1_features.f4_interop_casts

import kotlinx.cinterop.StableRef
import kotlin.native.runtime.GC

class HeavyObject(val data: IntArray)

fun box(): String {
    val obj = HeavyObject(IntArray(100) { it * 2 })
    val ref = StableRef.create(obj)

    for (i in 0 until 5) {
        val garbage = IntArray(1000) { it }
        GC.collect()
    }

    val retrieved = ref.get()
    if (retrieved.data.size != 100 || retrieved.data[50] != 100) {
        ref.dispose()
        return "FAIL: StableRef object corrupted after multiple GCs"
    }

    ref.dispose()
    return "OK"
}
