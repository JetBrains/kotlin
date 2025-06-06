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

fun fun3(l0: Any?): Any? {
    if (!tryEnterFrame()) {
        return null
    }
    var l1: Any? = Class0(l0)
    var l2: Any? = Class0(l1)
    var l3: Any? = Class0(l0)
    leaveFrame()
    return null
}

fun fun5(l0: Any?): Any? {
    if (!tryEnterFrame()) {
        return null
    }
    var l1: Any? = fun3(l0)
    var l2: Any? = fun3(l1)
    var l3: Any? = fun3(l0)
    leaveFrame()
    return null
}

fun fun7(l0: Any?): Any? {
    if (!tryEnterFrame()) {
        return null
    }
    var l1: Any? = l0
    var l2: Any? = l1
    var l3: Any? = l0
    leaveFrame()
    return null
}

fun fun9(l0: Any?): Any? {
    if (!tryEnterFrame()) {
        return null
    }
    var l1: Any? = null
    l1 = null
    l1 = null
    l1 = null
    leaveFrame()
    return null
}

fun mainBody() {
}
