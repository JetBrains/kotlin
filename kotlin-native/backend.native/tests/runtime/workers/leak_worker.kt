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
