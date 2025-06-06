@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlin.concurrent.atomics.ExperimentalAtomicApi::class)
package ktlib

import cinterop.*
import kotlin.concurrent.atomics.*
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

private val maxThreadsCount = AtomicInt(0)

private fun spawnThread(block: () -> Unit) {
   val allowedThreads = maxThreadsCount.fetchAndDecrement()
   if (allowedThreads <= 0) {
       maxThreadsCount.fetchAndIncrement()
       return
   }
   Worker.start().executeAfter(0L) {
       block()
       maxThreadsCount.fetchAndIncrement()
   }
}

private fun tryEnterFrame(localsCount: Int): Boolean {
    return localsCount < 500
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
    val nextLocalsCount = localsCount + 2
    if (!tryEnterFrame(nextLocalsCount)) {
        return null
    }
    g2 = l0
    l1?.storeField(0, g2?.loadField(1))
    spawnThread {
        val nextLocalsCount = 0
        fun6(nextLocalsCount, l1)
    }
    return null
}

fun fun6(localsCount: Int, l0: Any?): Any? {
    val nextLocalsCount = localsCount + 4
    if (!tryEnterFrame(nextLocalsCount)) {
        return null
    }
    var l1: Any? = Class0(null, null)
    var l2: Any? = Class1(null, null)
    var l3: Any? = fun7(nextLocalsCount, l0)
    return l1?.loadField(1)?.loadField(3)?.loadField(4)
}

fun mainBody() {
    val nextLocalsCount = 0
    var l0: Any? = Class1(null, null)
    var l1: Any? = l0
    var l2: Any? = fun5(nextLocalsCount, l0, l1?.loadField(67))
    var l3: Any? = fun6(nextLocalsCount, null)
}
