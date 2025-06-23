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

private var g2: Any? = null

fun fun4(localsCount: Int, l0: Any?, l1: Any?): Any? {
    g2 = l0
    l1?.storeField(0, g2?.loadField(1))
    spawnThread {
        val localsCount = 0
        call(localsCount, 4, { fun6(it, l1) })
    }
    return null
}

fun fun6(localsCount: Int, l0: Any?): Any? {
    var l1: Any? = alloc({ Class0(null, null) })
    var l2: Any? = alloc({ Class1(null, null) })
    var l3: Any? = call(localsCount, 4, { fun7(it, l0) })
    return l1?.loadField(1)?.loadField(3)?.loadField(4)
}

private fun mainBodyImpl(localsCount: Int) {
    var l0: Any? = alloc({ Class1(null, null) })
    var l1: Any? = l0
    var l2: Any? = call(localsCount, 2, { fun5(it, l0, l1?.loadField(67)) })
    var l3: Any? = call(localsCount, 4, { fun6(it, null) })
}

fun mainBody() {
    val localsCount = 0
    call(localsCount, 4, { mainBodyImpl(it) })
}
