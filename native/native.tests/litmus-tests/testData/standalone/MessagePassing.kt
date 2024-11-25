// IGNORE_NATIVE: cacheMode=STATIC_EVERYWHERE
// IGNORE_NATIVE: cacheMode=STATIC_PER_FILE_EVERYWHERE
// IGNORE_NATIVE: cacheMode=STATIC_USE_HEADERS_EVERYWHERE
// Without optimizations, this test runs too slowly.
// DISABLE_NATIVE: optimizationMode=DEBUG
// DISABLE_NATIVE: optimizationMode=NO

import kotlin.test.*
import org.jetbrains.litmuskt.*
import org.jetbrains.litmuskt.autooutcomes.*
import org.jetbrains.litmuskt.barriers.*
import org.jetbrains.litmuskt.tests.*
import kotlin.time.Duration.Companion.seconds

fun runTest(test: LitmusTest<*>) {
    val runner = WorkerRunner()
    val paramsList: List<LitmusRunParams> = variateRunParams(
        batchSizeSchedule = listOf(1_000_000),
        affinityMapSchedule = listOf(null),
        syncPeriodSchedule = listOf(10, 100), // sample params, should be ok for all tests
        barrierSchedule = listOf(::CinteropSpinBarrier)
    ).toList()

    val result = paramsList.map { params ->
        runner.runSingleTestParallel(test, params, timeLimit = 1.seconds, instances = 8)
    }.mergeResults()
    println(result.generateTable() + "\n")
    assertFalse { result.any { it.type == LitmusOutcomeType.FORBIDDEN } }
}

object MessagePassingTests {
    @Test
    fun plain() = runTest(MessagePassing.Plain)

    @Test
    fun volatileAnnotated() = runTest(MessagePassing.VolatileAnnotated)

    @Test
    fun raceFree() = runTest(MessagePassing.RaceFree)

    @Test
    fun missingVolatile() = runTest(MessagePassing.MissingVolatile)
}
