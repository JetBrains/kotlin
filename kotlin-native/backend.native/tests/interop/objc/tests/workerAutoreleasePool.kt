import kotlin.native.concurrent.*
import kotlinx.cinterop.*
import kotlin.test.*
import objcTests.*

@Test
fun testExecuteInMainPark() =
        test(Execute, InMain, Park)

@Test
fun testExecuteInMainProcessQueue() =
        test(Execute, InMain, ProcessQueue)

@Test
fun testExecuteInWorkerPark() =
        test(Execute, InWorker, Park)

@Test
fun testExecuteInWorkerProcessQueue() =
        test(Execute, InWorker, ProcessQueue)

@Test
fun testExecuteInMainToWorkerNoYield() =
        test(Execute, InMainToWorker, NoYield)

@Test
fun testExecuteAfter0InMainPark() =
        test(ExecuteAfter0, InMain, Park)

@Test
fun testExecuteAfter0InMainProcessQueue() =
        test(ExecuteAfter0, InMain, ProcessQueue)

@Test
fun testExecuteAfter0InWorkerPark() =
        test(ExecuteAfter0, InWorker, Park)

@Test
fun testExecuteAfter0InWorkerProcessQueue() =
        test(ExecuteAfter0, InWorker, ProcessQueue)

@Test
fun testExecuteAfter0InMainToWorkerNoYield() =
        test(ExecuteAfter0, InMainToWorker, NoYield)

@Test
fun testExecuteAfter10InMainPark() =
        testExecuteAfter10(InMain, Park)

@Test
fun testExecuteAfter10InMainProcessQueue() =
        testExecuteAfter10(InMain, ProcessQueue)

@Test
fun testExecuteAfter10InWorkerPark() =
        testExecuteAfter10(InWorker, Park)

@Test
fun testExecuteAfter10InWorkerProcessQueue() =
        testExecuteAfter10(InWorker, ProcessQueue)

@Test
fun testExecuteAfter10InMainToWorkerNoYield() =
        testExecuteAfter10(InMainToWorker, NoYield)

private fun testExecuteAfter10(context: Context, yieldMethod: Yield) =
        test(ExecuteAfter10(yieldMethod), context, yieldMethod)

private fun <F> test(method: ExecuteMethod<F>, context: Context, yieldMethod: Yield) {
    context.withWorker { worker ->
        fun execute(block: () -> Unit) {
            val future = method.submit(worker, block)
            yieldMethod.yield()
            method.wait(future)
        }

        val deallocated = CreateAutoreleaseDeallocated()

        execute {
            CreateAutorelease.createAutorelease(deallocated)
            // Object is still in autorelease pool:
            assertFalse(deallocated.value)
        }

        // autorelease pool is processed after the job is finished, so the object should be deallocated;
        // Checking in a job to make sure previous job is completely processed:
        execute {
            assertTrue(deallocated.value)
        }
    }
}

interface ExecuteMethod<F> {
    fun submit(worker: Worker, block: () -> Unit): F
    fun wait(future: F)
}

object Execute : ExecuteMethod<Future<Unit>> {
    override fun submit(worker: Worker, block: () -> Unit) = worker.execute(TransferMode.SAFE, { block.freeze() }) {
        it()
    }

    override fun wait(future: Future<Unit>) {
        future.result // Throws on failure.
    }
}

abstract class ExecuteAfter : ExecuteMethod<AtomicReference<Any?>> {
    abstract val timeout: Long

    abstract fun sleepAndYield()

    override fun submit(worker: Worker, block: () -> Unit): AtomicReference<Any?> {
        val result = AtomicReference<Any?>(null)

        worker.executeAfter(timeout, {
            try {
                block()
                result.value = true
            } catch (e: Throwable) {
                result.value = e.freeze()
            }
        }.freeze())

        return result
    }

    override fun wait(future: AtomicReference<Any?>) {
        while (true) {
            sleepAndYield()
            when (val it = future.value) {
                null -> continue
                true -> return
                else -> throw it as Throwable
            }
        }
    }
}

object ExecuteAfter0 : ExecuteAfter() {
    override val timeout = 0L

    override fun sleepAndYield() {
        // No sleep or additional yield required.
    }
}

class ExecuteAfter10(val yieldMethod: Yield) : ExecuteAfter() {
    override val timeout = 10L

    override fun sleepAndYield() {
        Worker.current.park(timeout + 1L, process = false)
        yieldMethod.yield()
    }
}

interface Context {
    fun withWorker(block: (worker: Worker) -> Unit)
}

object InMain : Context {
    override fun withWorker(block: (worker: Worker) -> Unit) = block(Worker.current)
}

object InMainToWorker : Context {
    override fun withWorker(block: (Worker) -> Unit) = kotlin.native.concurrent.withWorker {
        block(this)
    }
}

object InWorker : Context {
    override fun withWorker(block: (Worker) -> Unit) = kotlin.native.concurrent.withWorker {
        val method = Execute
        method.wait(method.submit(this) {
            block(this)
        })
    }
}

interface Yield {
    fun yield()
}

object Park : Yield {
    override fun yield() {
        Worker.current.park(0L, process = true)
    }
}

object ProcessQueue : Yield {
    override fun yield() {
        Worker.current.processQueue()
    }
}

object NoYield : Yield {
    override fun yield() {}
}
