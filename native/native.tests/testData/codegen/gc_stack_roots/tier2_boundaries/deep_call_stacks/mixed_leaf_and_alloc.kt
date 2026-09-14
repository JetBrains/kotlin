// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier2_boundaries.deep_call_stacks

import kotlin.native.runtime.GC

class FrameRoot(val level: Int)

fun leafFrame(v: Int): Int = (v * 3) xor 0x55

fun allocatingFrame(level: Int, maxLevel: Int): Int {
    val root = FrameRoot(level)
    val leafRes = leafFrame(level)
    if (level >= maxLevel) {
        GC.collect()
        return leafRes + root.level
    }
    val child = allocatingFrame(level + 1, maxLevel)
    return child + root.level + leafFrame(root.level)
}

fun box(): String {
    val result = allocatingFrame(1, 30)
    if (result == 0) return "FAIL mixed leaf and alloc"
    return "OK"
}
