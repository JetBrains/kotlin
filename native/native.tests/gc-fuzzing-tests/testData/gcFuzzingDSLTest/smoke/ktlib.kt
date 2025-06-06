@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package ktlib

import cinterop.*
import kotlin.native.concurrent.*

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

private fun spawnThread(block: () -> Unit) {
   Worker.start().executeAfter(0L, block)
}

@ThreadLocal
private var frameCount = 100;

private fun tryEnterFrame(): Boolean {
    if (frameCount-- <= 0) {
        frameCount++
        return false
    }
    return true
}

private fun leaveFrame() {
    frameCount++
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

fun fun4(l0: Any?, l1: Any?): Any? {
    if (!tryEnterFrame()) {
        return null
    }
    g2 = l0
    l1?.storeField(0, g2?.loadField(1))
    spawnThread {
        fun6(l1)
    }
    leaveFrame()
    return null
}

fun fun6(l0: Any?): Any? {
    if (!tryEnterFrame()) {
        return null
    }
    var l1: Any? = Class0(null, null)
    var l2: Any? = Class1(null, null)
    var l3: Any? = fun7(l0)
    leaveFrame()
    return l1?.loadField(1)?.loadField(3)?.loadField(4)
}

fun mainBody() {
    var l0: Any? = Class1(null, null)
    var l1: Any? = l0
    var l2: Any? = fun5(l0, l1?.loadField(67))
    var l3: Any? = fun6(null)
}
