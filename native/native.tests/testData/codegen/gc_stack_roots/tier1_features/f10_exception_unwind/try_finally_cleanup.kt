// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier1_features.f10_exception_unwind

import kotlin.native.runtime.GC

class Resource(val name: String, var closed: Boolean = false)

fun useResourceNormally(): String {
    val res = Resource("db_conn")
    try {
        GC.collect()
        return res.name
    } finally {
        res.closed = true
    }
}

fun box(): String {
    val name = useResourceNormally()
    if (name != "db_conn") return "FAIL try_finally_cleanup: $name"
    return "OK"
}
