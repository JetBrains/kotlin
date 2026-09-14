// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier3_combinations

import kotlin.native.runtime.GC

class InlinedCarrier(val tag: String)

inline fun <T, R> processWithInlineSafepoint(item: T, transform: (T) -> R): R {
    GC.collect()
    val res = transform(item)
    GC.collect()
    return res
}

fun box(): String {
    val carrier = InlinedCarrier("payload_inline")
    val transformed = processWithInlineSafepoint(carrier) { c ->
        c.tag + "_processed"
    }

    if (transformed != "payload_inline_processed") {
        return "FAIL addrspace with inlining: $transformed"
    }
    return "OK"
}
