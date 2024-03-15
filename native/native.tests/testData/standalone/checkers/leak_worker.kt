// EXIT_CODE: !0
// OUTPUT_REGEX: Unfinished workers detected, 1 workers leaked!.*
@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class, ExperimentalForeignApi::class)
import kotlin.native.concurrent.*
import kotlin.native.Platform
import kotlinx.cinterop.*

fun main() {
    Platform.isMemoryLeakCheckerActive = true
    val worker = Worker.start()
    // Make sure worker is initialized.
    worker.execute(TransferMode.SAFE, {}, {}).result;
    StableRef.create(Any())
}
