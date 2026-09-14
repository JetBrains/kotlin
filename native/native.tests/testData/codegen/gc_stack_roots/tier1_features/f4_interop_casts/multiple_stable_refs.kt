// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier1_features.f4_interop_casts

import kotlinx.cinterop.StableRef
import kotlin.native.runtime.GC

class Item(val id: Int)

fun box(): String {
    val refs = ArrayList<StableRef<Item>>()
    for (i in 0 until 20) {
        refs.add(StableRef.create(Item(i)))
    }

    GC.collect()

    for (i in 0 until 20) {
        val item = refs[i].get()
        if (item.id != i) {
            refs.forEach { it.dispose() }
            return "FAIL: ref $i corrupted"
        }
    }

    refs.forEach { it.dispose() }
    return "OK"
}
