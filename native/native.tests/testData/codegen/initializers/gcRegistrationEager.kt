// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_STAGE: Native:*
// KIND: STANDALONE
// GC from @EagerInitialization may interfere with other tests
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class, kotlin.experimental.ExperimentalNativeApi::class, kotlin.ExperimentalStdlibApi::class)

import kotlin.native.runtime.GC
import kotlin.native.ref.WeakReference

class C(val x: Int)

@EagerInitialization
val weakX = initX()
@EagerInitialization
val xStatus = checkX()
@EagerInitialization
var x: C? = null

@NoInline
fun initX(): WeakReference<C> {
    x = C(42)
    return WeakReference(x!!)
}

fun checkX(): String {
    GC.collect()
    val v = weakX.value?.x
    return when (v) {
        42 -> "OK"
        else -> "FAIL: $v"
    }
}

fun box(): String {
    return xStatus
}
