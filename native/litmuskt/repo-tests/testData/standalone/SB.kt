// KIND: STANDALONE

import kotlin.test.*
import komem.litmus.*
import komem.litmus.barriers.*
import komem.litmus.tests.*

fun runTest(test: LitmusTest<*>) {
    val runner = WorkerRunner()
    val params = LitmusRunParams(
        batchSize = 10_000_000,
        syncPeriod = 500,
        affinityMap = null,
        barrierProducer = ::CinteropSpinBarrier
    )
    val results = runner.runTest(params, test)
    println(results.generateTable())
    assertEquals(4, results.size)
}

@Test
fun sb() {
    runTest(SB)
}
