// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier1_features.f11_addrspace_lowering

import kotlin.native.runtime.GC

class ConfigHolder {
    companion object {
        var activeConfig: String = "init_cfg"
    }
}

fun box(): String {
    ConfigHolder.activeConfig = "runtime_cfg"

    GC.collect()

    if (ConfigHolder.activeConfig != "runtime_cfg") {
        return "FAIL companion property: ${ConfigHolder.activeConfig}"
    }
    return "OK"
}
