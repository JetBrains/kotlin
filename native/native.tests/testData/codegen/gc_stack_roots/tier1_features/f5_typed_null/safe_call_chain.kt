// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier1_features.f5_typed_null

import kotlin.native.runtime.GC

class StepC(val name: String)
class StepB(val c: StepC?)
class StepA(val b: StepB?)

fun box(): String {
    val nonNullChain = StepA(StepB(StepC("deep_value")))
    val nullMidChain = StepA(StepB(null))
    val nullStartChain: StepA? = null

    GC.collect()

    val r1 = nonNullChain.b?.c?.name
    val r2 = nullMidChain.b?.c?.name
    val r3 = nullStartChain?.b?.c?.name

    if (r1 != "deep_value") return "FAIL r1: $r1"
    if (r2 != null) return "FAIL r2: $r2"
    if (r3 != null) return "FAIL r3: $r3"

    return "OK"
}
