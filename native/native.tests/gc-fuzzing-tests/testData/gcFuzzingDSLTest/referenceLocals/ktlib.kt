@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlin.native.runtime.NativeRuntimeApi::class)
package ktlib

import cinterop.*
import kotlin.native.concurrent.Worker

interface KotlinIndexAccess {
   fun loadKotlinField(index: Int): Any?
   fun storeKotlinField(index: Int, value: Any?)
}

private fun Any.loadField(index: Int) = when (this) {
    is KotlinIndexAccess -> loadKotlinField(index)
    is ObjCIndexAccessProtocol -> loadObjCField(index)
    else -> error("Invalid loadField call")
}

private fun Any.storeField(index: Int, value: Any?) = when (this) {
    is KotlinIndexAccess -> storeKotlinField(index, value)
    is ObjCIndexAccessProtocol -> storeObjCField(index, value)
    else -> error("Invalid storeField call")
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
    val nextLocalsCount = localsCount + blockLocalsCount
    if (nextLocalsCount > 500) {
        return null
    }
    return block(nextLocalsCount)
}

var allocBlocker: Boolean = false

fun performGC() { kotlin.native.runtime.GC.collect() }

private inline fun alloc(block: () -> Any?): Any? {
    if (!allocBlocker || !updateAllocBlocker()) return block()
    return null
}

class Class0(var f0: Any?) : KotlinIndexAccess {
    override fun loadKotlinField(index: Int): Any? {
        return when (index % 1) {
            0 -> f0
            else -> null
        }
    }

    override fun storeKotlinField(index: Int, value: Any?) {
        when (index % 1) {
            0 -> f0 = value
        }
    }
}

private var g1: Any? = null

fun fun3(localsCount: Int, l0: Any?): Any? {
    var l1: Any? = alloc({ Class0(l0) })
    var l2: Any? = alloc({ Class0(l1) })
    var l3: Any? = alloc({ Class0(l0) })
    return null
}

fun fun5(localsCount: Int, l0: Any?): Any? {
    var l1: Any? = call(localsCount, 4, { fun3(it, l0) })
    var l2: Any? = call(localsCount, 4, { fun3(it, l1) })
    var l3: Any? = call(localsCount, 4, { fun3(it, l0) })
    return null
}

fun fun7(localsCount: Int, l0: Any?): Any? {
    var l1: Any? = l0
    var l2: Any? = l1
    var l3: Any? = l0
    return null
}

fun fun9(localsCount: Int, l0: Any?): Any? {
    var l1: Any? = null
    l1 = null
    l1 = null
    l1 = null
    return null
}

private fun mainBodyImpl(localsCount: Int) {
    var l0: Any? = call(localsCount, 4, { fun3(it, null) })
    var l1: Any? = call(localsCount, 4, { fun4(it, null) })
}

fun mainBody() {
    val localsCount = 0
    call(localsCount, 2, { mainBodyImpl(it) })
}
