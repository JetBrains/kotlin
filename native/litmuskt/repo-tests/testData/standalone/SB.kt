// KIND: STANDALONE

import kotlin.test.*
import org.jetbrains.litmuskt.*
import org.jetbrains.litmuskt.autooutcomes.*
import org.jetbrains.litmuskt.barriers.*
import org.jetbrains.litmuskt.tests.*

fun runTest(test: LitmusTest<*>) {
    val runner = WorkerRunner()
    val params = LitmusRunParams(
        batchSize = 1_000_000,
        syncPeriod = 50,
        affinityMap = null,
        barrierProducer = ::CinteropSpinBarrier
    )
    val results = runner.runTests(listOf(test), params).first()
//    println(results.generateTable())
    assertEquals(4, results.size)
}

@Test
fun sb() {
    runTest(StoreBuffering.Plain)
}
