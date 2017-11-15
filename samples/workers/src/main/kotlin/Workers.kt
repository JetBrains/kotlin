import konan.worker.*

data class WorkerArgument(val intParam: Int, val stringParam: String)
data class WorkerResult(val intResult: Int, val stringResult: String)

fun main(args: Array<String>) {
    val COUNT = 5
    val workers = Array(COUNT, { _ -> startWorker()})

    for (attempt in 1 .. 3) {
        val futures = Array(workers.size, { workerIndex -> workers[workerIndex].schedule(TransferMode.CHECKED, {
            WorkerArgument(workerIndex, "attempt $attempt") }) { input ->
                var sum = 0
                for (i in 0..input.intParam * 1000) {
                    sum += i
                }
                WorkerResult(sum, input.stringParam + " result")
            }
        })
        val futureSet = futures.toSet()
        var consumed = 0
        while (consumed < futureSet.size) {
            val ready = futureSet.waitForMultipleFutures(10000)
            ready.forEach {
                it.consume { result ->
                    if (result.stringResult != "attempt $attempt result") throw Error("Unexpected $result")
                    consumed++ }
            }
        }
    }
    workers.forEach {
        it.requestTermination().consume { _ -> }
    }
    println("OK")
}
