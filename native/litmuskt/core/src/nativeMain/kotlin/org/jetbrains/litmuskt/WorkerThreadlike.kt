package org.jetbrains.litmuskt

import kotlin.native.concurrent.ObsoleteWorkersApi
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker

@OptIn(ObsoleteWorkersApi::class)
class WorkerThreadlike : Threadlike {

    val worker = Worker.start()

    private data class WorkerContext<A>(
        val args: A,
        val threadFunction: (A) -> Unit,
    )

    override fun <A : Any> start(args: A, function: (A) -> Unit): BlockingFuture {
        val context = WorkerContext(args, function)
        val future = worker.execute(
            TransferMode.SAFE /* ignored */,
            { context }
        ) { (a, f) -> f(a) }
        return BlockingFuture { future.result }
    }

    override fun dispose() {
        worker.requestTermination().result
    }
}
