// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier3_combinations

import kotlinx.cinterop.StableRef
import kotlin.native.runtime.GC

class NativeBridgeData(var counter: Int)

fun passToNativeAndBack(data: NativeBridgeData): Int {
    val ref = StableRef.create(data)
    try {
        GC.collect()
        val retrieved = ref.get()
        retrieved.counter += 10
        return retrieved.counter
    } finally {
        ref.dispose()
    }
}

fun box(): String {
    val data = NativeBridgeData(5)
    val res = passToNativeAndBack(data)

    GC.collect()

    if (res != 15 || data.counter != 15) {
        return "FAIL addrspace with cinterop: res=$res, counter=${data.counter}"
    }
    return "OK"
}
