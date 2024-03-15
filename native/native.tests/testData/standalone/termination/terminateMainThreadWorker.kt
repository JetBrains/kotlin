// OUTPUT_REGEX: ^$
@file:OptIn(FreezingIsDeprecated::class, ObsoleteWorkersApi::class)

import kotlin.test.*

import kotlin.native.concurrent.*

// Ensure that termination of current worker on main thread doesn't lead to problems.
fun main() {
    val worker = Worker.current
    val future = worker.requestTermination(false)
    worker.processQueue()
    assertEquals(future.state, FutureState.COMPUTED)
    future.consume {}
    // After termination request this worker is no longer addressable.
    assertFailsWith<IllegalStateException> { worker.executeAfter(0, {
        println("BUG!")
    })}
}
