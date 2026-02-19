// No termination is going on here. But that's the closest location to other unhandled exception hook tests.
// KIND: REGULAR
// OUTPUT_REGEX: hook called\R
import kotlin.test.*

import kotlin.native.concurrent.*

@OptIn(kotlin.experimental.ExperimentalNativeApi::class)
@Test
fun testExecuteAfterStartQuiet() {
    setUnhandledExceptionHook {
        println("hook called")
    }
    val worker = Worker.start()
    worker.executeAfter(0L, {
        throw Error("an error")
    })
    worker.requestTermination().result
}