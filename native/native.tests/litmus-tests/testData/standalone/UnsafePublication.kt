// IGNORE_NATIVE: cacheMode=STATIC_EVERYWHERE
// IGNORE_NATIVE: cacheMode=STATIC_PER_FILE_EVERYWHERE
// IGNORE_NATIVE: cacheMode=STATIC_USE_HEADERS_EVERYWHERE
// Without optimizations, this test runs too slowly.
// DISABLE_NATIVE: optimizationMode=DEBUG
// DISABLE_NATIVE: optimizationMode=NO
// With the aggressive scheduler, this test runs too slowly.
// DISABLE_NATIVE: gcScheduler=AGGRESSIVE

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

object UnsafePublicationTests {
    @Test
    fun plain() = runTest(UnsafePublication.Plain)

    @Test
    fun volatileAnnotated() = runTest(UnsafePublication.VolatileAnnotated)

    @Test
    fun plainWithConstructor() = runTest(UnsafePublication.PlainWithConstructor)

    @Test
    fun plainIntArray() = runTest(UnsafePublication.PlainIntArray)

    @Test
    @Ignore // Fails, because Kotlin does not have a full construction guarantee.
    fun plainArray() = runTest(UnsafePublicationNative.PlainArray)

    @Test
    @Ignore // Fails, because Kotlin does not have a full construction guarantee.
    fun reference() = runTest(UnsafePublication.Reference)

    @Test
    fun plainWithLeakingConstructor() = runTest(UnsafePublication.PlainWithLeakingConstructor)
}
