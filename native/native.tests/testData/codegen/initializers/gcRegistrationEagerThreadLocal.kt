// KIND: STANDALONE
// GC from @EagerInitialization may interfere with other tests
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class, kotlin.experimental.ExperimentalNativeApi::class, kotlin.ExperimentalStdlibApi::class)

import kotlin.concurrent.Volatile
import kotlin.native.concurrent.ThreadLocal
import kotlin.native.runtime.GC
import kotlin.native.ref.WeakReference

class C(val x: Int)

@EagerInitialization
@Volatile
var threadCounter = 0

@ThreadLocal
@EagerInitialization
val weakX = initX()
@ThreadLocal
@EagerInitialization
val xStatus = checkX()
@ThreadLocal
@EagerInitialization
var x: C? = null

@NoInline
fun initX(): WeakReference<C> {
    x = C(42)
    return WeakReference(x!!)
}

fun checkX(): String {
    val threadId = threadCounter++
    if (threadId == 0) {
        GC.collect() // See KT-89843
    }
    val v = weakX.value?.x
    return when (v) {
        42 -> "OK"
        else -> "FAIL: $v"
    }
}

fun box(): String {
    return xStatus
}
