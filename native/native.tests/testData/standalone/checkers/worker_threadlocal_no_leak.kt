@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class, ObsoleteWorkersApi::class)
import kotlin.native.concurrent.*
import kotlin.native.Platform

@ThreadLocal
var x = Any()

fun main() {
    Platform.isMemoryLeakCheckerActive = true
    val worker = Worker.start()

    worker.execute(TransferMode.SAFE, {}) {
        println(x)  // Make sure x is initialized
    }.result

    worker.requestTermination().result
}
