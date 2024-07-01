package org.jetbrains.litmuskt

import org.jetbrains.litmuskt.barriers.*
import kotlin.time.Duration.Companion.seconds

fun runTestWithSampleParams(test: LitmusTest<*>): LitmusResult {
    val runner = WorkerRunner()
    // Later there should be more variations for less frequent but longer test runs
    val paramsList: List<LitmusRunParams> = variateRunParams(
        batchSizeSchedule = listOf(1_000_000),
        affinityMapSchedule = listOf(null),
        syncPeriodSchedule = listOf(10, 100),
        barrierSchedule = listOf(::CinteropSpinBarrier)
    ).toList()

    val result = paramsList.map { params ->
        runner.runSingleTestParallel(test, params, timeLimit = 1.seconds, instances = 3)
    }.mergeResults()
    return result
}

fun LitmusResult.hasForbidden() = any { it.type == LitmusOutcomeType.FORBIDDEN }
