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

class Class0(var f0: Any?, var f1: Any?) : KotlinIndexAccess {
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


fun fun2(localsCount: Int): Any? {
    var l0: Any? = alloc({ Class0(null, null) })
    var l1: Any? = alloc({ Class1(null, null) })
    l0?.storeField(0, l1?.loadField(1))
    l0?.storeField(1, l1?.loadField(0))
    l1?.storeField(0, l0?.loadField(1))
    l1?.storeField(1, l0?.loadField(0))
    return null
}

private fun mainBodyImpl(localsCount: Int) {
    var l0: Any? = call(localsCount, 2, { fun2(it) })
    var l1: Any? = call(localsCount, 2, { fun3(it) })
}

fun mainBody() {
    val localsCount = 0
    call(localsCount, 2, { mainBodyImpl(it) })
}
