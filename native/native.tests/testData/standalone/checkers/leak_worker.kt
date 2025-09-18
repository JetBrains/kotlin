// EXIT_CODE: !0
// OUTPUT_REGEX: .*Unfinished workers detected, 1 workers leaked!.*
@file:OptIn(ExperimentalStdlibApi::class)
import kotlin.native.concurrent.*

fun main() {
    val worker = Worker.start()
    // Make sure worker is initialized.
    worker.execute(TransferMode.SAFE, {}, {}).result;

    val activeWorkersCount = Worker.activeWorkers.size
    check(activeWorkersCount == 1) {
        "Unfinished workers detected, ${activeWorkersCount - 1} workers leaked!"
    }
}
