@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class, kotlin.experimental.ExperimentalNativeApi::class)

import kotlin.native.concurrent.ThreadLocal
import kotlin.native.runtime.GC
import kotlin.native.ref.WeakReference

class C(val x: Int)

@ThreadLocal
val weakX = initX()
@ThreadLocal
val xStatus = checkX()
@ThreadLocal
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
