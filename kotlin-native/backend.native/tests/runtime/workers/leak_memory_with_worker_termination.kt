import kotlin.native.concurrent.*
import kotlinx.cinterop.*

fun main() {
    val worker = Worker.start()
    // Make sure worker is initialized.
    worker.execute(TransferMode.SAFE, {}, {}).result;
    StableRef.create(Any())
    worker.requestTermination().result
}
