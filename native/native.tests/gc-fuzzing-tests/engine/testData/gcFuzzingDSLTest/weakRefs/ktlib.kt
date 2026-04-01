@file:OptIn(
    kotlinx.cinterop.ExperimentalForeignApi::class,
    kotlin.native.runtime.NativeRuntimeApi::class,
    kotlin.experimental.ExperimentalNativeApi::class
)

package ktlib

import cinterop.*
import kotlin.native.concurrent.Worker
import kotlin.native.ref.WeakReference

interface KotlinIndexAccess {
   fun loadKotlinField(index: Int): Any?
   fun storeKotlinField(index: Int, value: Any?)
}

private fun Any.loadField(index: Int) = when (this) {
    is KotlinIndexAccess -> loadKotlinField(index)
    is ObjCIndexAccessProtocol -> loadObjCField(index)
    else -> error("Invalid loadField call " + this)
}

private fun Any.storeField(index: Int, value: Any?) = when (this) {
    is KotlinIndexAccess -> storeKotlinField(index, value)
    is ObjCIndexAccessProtocol -> storeObjCField(index, value)
    else -> error("Invalid storeField call " + this)
}

object WorkerTerminationProcessor {
    private val processor = Worker.start()

    fun terminateCurrent() {
        val future = Worker.current.requestTermination()
        processor.executeAfter(0L) { future.result }
    }
}

private fun spawnThread(block: () -> Unit) {
   if (!tryRegisterThread())
       return
   Worker.start().executeAfter(0L) {
       block()
       WorkerTerminationProcessor.terminateCurrent()
       unregisterThread()
   }
}

private inline fun call(localsCount: Int, blockLocalsCount: Int, block: (Int) -> Any?): Any? {
    if (terminationRequest) return null
    val nextLocalsCount = localsCount + blockLocalsCount
    if (nextLocalsCount > 200) {
        return null
    }
    return block(nextLocalsCount)
}

var allocBlocker: Boolean = false
var terminationRequest: Boolean = false

fun performGC() { kotlin.native.runtime.GC.collect() }

private inline fun alloc(block: () -> Any?): Any? {
    if (!allocBlocker || !updateAllocBlocker()) return block()
    return null
}

private var g0Impl: WeakReference<Any>? = null?.let { WeakReference(it) }
private var g0: Any?
    get() = g0Impl?.value
    set(value) { g0Impl = value?.let { WeakReference(it) } }
class Class2(f0: Any?, f1: Any?) : KotlinIndexAccess {
    var f0: Any? = f0
    private var f1Impl: WeakReference<Any>? = f1?.let { WeakReference(it) }
    var f1: Any?
        get() = f1Impl?.value
        set(value) { f1Impl = value?.let { WeakReference(it) } }
    override fun loadKotlinField(index: Int): Any? {
        return when (index % 2) {
            0 -> f0
            1 -> f1
            else -> null
        }
    }

    override fun storeKotlinField(index: Int, value: Any?) {
        when (index % 2) {
            0 -> f0 = value
            1 -> f1 = value
        }
    }
}


fun fun4(localsCount: Int): Any? {
    var l0: Any? = alloc({ Class2(g0, g0) })
    l0?.storeField(0, l0?.loadField(1))
    l0?.storeField(1, l0?.loadField(0))
    l0?.storeField(0, l0?.loadField(1))
    l0?.storeField(1, l0?.loadField(0))
    return null
}

private fun mainBodyImpl(localsCount: Int) {
    var l0: Any? = alloc({ Class2(null, null) })
    g0 = l0
    g0 = l0
    var l1: Any? = call(localsCount, 1, { fun4(it) })
    var l2: Any? = call(localsCount, 1, { fun5(it) })
}

fun mainBody() {
    val localsCount = 0
    call(localsCount, 3, { mainBodyImpl(it) })
}
