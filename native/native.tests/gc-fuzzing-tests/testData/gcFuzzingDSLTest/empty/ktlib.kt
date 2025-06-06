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


fun mainBody() {
    val nextLocalsCount = 0
}
