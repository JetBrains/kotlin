// No termination is going on here. But that's the closest location to other unhandled exception hook tests.
// KIND: REGULAR
// OUTPUT_REGEX: (?!.*an error.*)
import kotlin.test.*

import kotlin.native.concurrent.*

@Test
fun testExecuteAfterStartQuiet() {
    val worker = Worker.start(errorReporting = false)
    worker.executeAfter(0L, {
        throw Error("an error")
    })
    worker.requestTermination().result
}