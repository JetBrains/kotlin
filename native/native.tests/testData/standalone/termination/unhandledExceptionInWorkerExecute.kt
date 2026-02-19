// No termination is going on here. But that's the closest location to other unhandled exception hook tests.
// KIND: REGULAR
// OUTPUT_REGEX: .*an error.*
import kotlin.test.*

import kotlin.native.concurrent.*

@Test
fun testExecuteStart() {
    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, {}) {
        throw Error("an error")
    }
    assertFailsWith<Throwable> {
        future.result
    }
    worker.requestTermination().result
}