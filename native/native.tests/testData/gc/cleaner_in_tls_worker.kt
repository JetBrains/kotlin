// DISABLE_NATIVE: gcType=NOOP
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative,kotlin.native.runtime.NativeRuntimeApi,kotlin.experimental.ExperimentalNativeApi,kotlinx.cinterop.ExperimentalForeignApi

import kotlin.test.*

import kotlin.concurrent.AtomicInt
import kotlin.native.concurrent.*
import kotlin.native.internal.*
import kotlin.native.ref.Cleaner
import kotlin.native.ref.createCleaner
import kotlin.native.runtime.GC

@ThreadLocal
var tlsCleaner: Cleaner? = null

val value = AtomicInt(0)

@Test
fun test() {
    val worker = Worker.start()

    worker.execute(TransferMode.SAFE, {}) {
        tlsCleaner = createCleaner(42) {
            value.value = it
        }
    }

    worker.requestTermination().result
    waitWorkerTermination(worker)
    GC.collect()
    waitCleanerWorker()

    assertEquals(42, value.value)
}
