@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)

import objclib.*
import kotlin.native.concurrent.*
import kotlinx.cinterop.*

fun main() {
    val worker = Worker.start()
    worker.execute(TransferMode.SAFE, {}) {
        val withFinalizer = WithFinalizer()
        val finalizer: Finalizer = staticCFunction { ptr: COpaquePointer? ->
            ptr?.asStableRef<Any>()?.dispose()
            println("Executed finalizer")
        }
        val arg = StableRef.create(Any()).asCPointer()
        withFinalizer.setFinalizer(finalizer, arg)
    }.result
    worker.requestTermination().result
    waitWorkerTermination(worker)

    if (Platform.memoryModel == MemoryModel.EXPERIMENTAL) {
        // Experimental MM by default doesn't run GC neither on worker termination nor on program exit.
        // Enforce GC on program exit:
        kotlin.native.runtime.Debugging.forceCheckedShutdown = true
    }
}
