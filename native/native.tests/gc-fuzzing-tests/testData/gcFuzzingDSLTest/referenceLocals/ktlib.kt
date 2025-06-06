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

private fun tryEnterFrame(localsCount: Int): Boolean {
    return localsCount < 500
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
    val nextLocalsCount = localsCount + 4
    if (!tryEnterFrame(nextLocalsCount)) {
        return null
    }
    var l1: Any? = Class0(l0)
    var l2: Any? = Class0(l1)
    var l3: Any? = Class0(l0)
    return null
}

fun fun5(localsCount: Int, l0: Any?): Any? {
    val nextLocalsCount = localsCount + 4
    if (!tryEnterFrame(nextLocalsCount)) {
        return null
    }
    var l1: Any? = fun3(nextLocalsCount, l0)
    var l2: Any? = fun3(nextLocalsCount, l1)
    var l3: Any? = fun3(nextLocalsCount, l0)
    return null
}

fun fun7(localsCount: Int, l0: Any?): Any? {
    val nextLocalsCount = localsCount + 4
    if (!tryEnterFrame(nextLocalsCount)) {
        return null
    }
    var l1: Any? = l0
    var l2: Any? = l1
    var l3: Any? = l0
    return null
}

fun fun9(localsCount: Int, l0: Any?): Any? {
    val nextLocalsCount = localsCount + 2
    if (!tryEnterFrame(nextLocalsCount)) {
        return null
    }
    var l1: Any? = null
    l1 = null
    l1 = null
    l1 = null
    return null
}

fun mainBody() {
    val nextLocalsCount = 0
    var l0: Any? = fun3(nextLocalsCount, null)
    var l1: Any? = fun4(nextLocalsCount, null)
}
