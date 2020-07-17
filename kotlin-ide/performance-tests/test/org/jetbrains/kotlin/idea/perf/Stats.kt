/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf

import org.jetbrains.kotlin.idea.perf.WholeProjectPerformanceTest.Companion.nsToMs
import org.jetbrains.kotlin.idea.perf.profilers.*
import org.jetbrains.kotlin.idea.perf.util.TeamCity
import org.jetbrains.kotlin.idea.perf.util.logMessage
import org.jetbrains.kotlin.idea.testFramework.suggestOsNeutralFileName
import org.jetbrains.kotlin.test.KotlinRoot
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.util.PerformanceCounter
import java.io.*
import kotlin.system.measureNanoTime
import java.lang.ref.WeakReference
import java.nio.file.Paths
import kotlin.math.*
import kotlin.system.measureTimeMillis
import kotlin.test.assertEquals

typealias StatInfos = Map<String, Any>?

class Stats(
    val name: String = "",
    private val profilerConfig: ProfilerConfig = ProfilerConfig(),
    private val header: Array<String> = arrayOf("Name", "ValueMS", "StdDev"),
    private val acceptanceStabilityLevel: Int = 25
) : Closeable {

    private val perfTestRawDataMs = mutableListOf<Long>()

    private val statsFile: File = pathToResource("stats${statFilePrefix()}.csv")

    private val statsOutput: BufferedWriter

    init {
        statsOutput = statsFile.bufferedWriter()

        statsOutput.appendLine(header.joinToString())

        PerformanceCounter.setTimeCounterEnabled(true)
    }

    private fun statFilePrefix() = if (name.isNotEmpty()) "-${plainname()}" else ""

    private fun plainname() = suggestOsNeutralFileName(name)

    private fun pathToResource(resource: String) = Paths.get(KotlinRoot.PATH, "../out/$resource").normalize().toAbsolutePath().toFile()

    private fun append(id: String, statInfosArray: Array<StatInfos>) {
        val timingsMs = toTimingsMs(statInfosArray)

        val calcMean = calcMean(timingsMs)

        for (v in listOf(
            Triple("mean", "", calcMean.mean.toLong()),
            Triple("stdDev", " stdDev", calcMean.stdDev.toLong()),
            Triple("geomMean", " geomMean", calcMean.geomMean.toLong())
        )) {
            val n = "$id : ${v.first}"

            TeamCity.test(n, durationMs = v.third, includeStats = false) {
                TeamCity.statValue("$id${v.second}", v.third)
            }
        }

        statInfosArray.filterNotNull()
            .map { it.keys }
            .fold(mutableSetOf<String>(), { acc, keys -> acc.addAll(keys); acc })
            .filter { it != TEST_KEY && it != ERROR_KEY }
            .sorted().forEach { perfCounterName ->
                val values = statInfosArray.map { (it?.get(perfCounterName) as? Long) ?: 0L }.toLongArray()
                val statInfoMean = calcMean(values)

                val n = "$id : $perfCounterName"
                val mean = statInfoMean.mean.toLong()

                val shortName = if (perfCounterName.endsWith(": time")) n.removeSuffix(": time") else null

                TeamCity.test(shortName, durationMs = mean) {
                    TeamCity.statValue(n, mean)
                }
            }

        perfTestRawDataMs.addAll(timingsMs.toList())
        append(arrayOf(id, calcMean.mean, calcMean.stdDev))
    }

    private fun toTimingsMs(statInfosArray: Array<StatInfos>) =
        statInfosArray.map { info -> info?.let { it[TEST_KEY] as? Long }?.nsToMs ?: 0L }.toLongArray()

    private fun calcMean(statInfosArray: Array<StatInfos>): Mean = calcMean(toTimingsMs(statInfosArray))

    private fun calcMean(values: LongArray): Mean {
        val mean = values.average()

        val stdDev = if (values.size > 1) (sqrt(
            values.fold(0.0,
                        { accumulator, next -> accumulator + (1.0 * (next - mean)).pow(2) })
        ) / (values.size - 1))
        else 0.0

        val geomMean = geomMean(values.toList())

        return Mean(mean, stdDev, geomMean)
    }

    data class Mean(val mean: Double, val stdDev: Double, val geomMean: Double)

    private fun append(values: Array<Any>) {
        require(values.size == header.size) { "Expected ${header.size} values, actual ${values.size} values" }
        with(statsOutput) {
            appendLine(values.joinToString { it.toString() })
            flush()
        }
    }

    fun append(file: String, id: String, nanoTime: Long) {
        val ms = nanoTime.nsToMs
        append(arrayOf(file, id, ms))
    }

    fun <SV, TV> perfTest(
        testName: String,
        warmUpIterations: Int = 5,
        iterations: Int = 20,
        setUp: (TestData<SV, TV>) -> Unit = { },
        test: (TestData<SV, TV>) -> Unit,
        tearDown: (TestData<SV, TV>) -> Unit = { },
        checkStability: Boolean = true
    ) {

        val warmPhaseData = PhaseData(
            iterations = warmUpIterations,
            testName = testName,
            setUp = setUp,
            test = test,
            tearDown = tearDown
        )
        val mainPhaseData = PhaseData(
            iterations = iterations,
            testName = testName,
            setUp = setUp,
            test = test,
            tearDown = tearDown
        )
        val block = {
            warmUpPhase(warmPhaseData)
            val statInfoArray = mainPhase(mainPhaseData)

            assertEquals(iterations, statInfoArray.size)
            if (testName != WARM_UP) {
                appendTimings(testName, statInfoArray)

                // do not estimate stability for warm-up
                if (!testName.contains(WARM_UP)) {
                    val calcMean = calcMean(statInfoArray)
                    val stabilityPercentage = round(calcMean.stdDev * 100.0 / calcMean.mean).toInt()
                    logMessage { "$testName stability is $stabilityPercentage %" }
                    val stabilityName = "$name: $testName stability"

                    val stable = stabilityPercentage <= acceptanceStabilityLevel

                    val error = if (stable or !checkStability) {
                        null
                    } else {
                        "$testName stability is $stabilityPercentage %, above accepted level of $acceptanceStabilityLevel %"
                    }

                    TeamCity.test(stabilityName, errorDetails = error) {
                        TeamCity.statValue(stabilityName, stabilityPercentage)
                    }
                }
            } else {
                printTimings(testName, printOnlyErrors = true, statInfoArray = statInfoArray)
            }
        }

        if (testName != WARM_UP) {
            TeamCity.suite(testName, block)
        } else {
            block()
        }
    }

    private fun printTimings(
        prefix: String,
        statInfoArray: Array<StatInfos>,
        printOnlyErrors: Boolean = false,
        attemptFn: (Int) -> String = { attempt -> "#$attempt" }
    ) {
        for (statInfoIndex in statInfoArray.withIndex()) {
            val attempt = statInfoIndex.index
            val statInfo = statInfoIndex.value ?: continue
            val n = "$name: $prefix ${attemptFn(attempt)}"

            val t = statInfo[ERROR_KEY] as? Throwable
            if (t != null) {
                TeamCity.test(n, errors = listOf(t)) {}
            } else if (!printOnlyErrors) {
                TeamCity.test(n, durationMs = (statInfo[TEST_KEY] as Long).nsToMs) {
                    for ((k, v) in statInfo) {
                        if (k == TEST_KEY) continue
                        TeamCity.statValue("$n $k", v)
                        (v as? Number)?.let {
                            TeamCity.metadata(n, k, it)
                        }
                    }
                }
            }
        }
    }

    fun printWarmUpTimings(
        prefix: String,
        warmUpStatInfosArray: Array<StatInfos>
    ) = printTimings(prefix, warmUpStatInfosArray) { attempt -> "warm-up #$attempt" }

    fun appendTimings(
        prefix: String,
        statInfosArray: Array<StatInfos>
    ) {
        printTimings(prefix, statInfosArray)
        append("$name: $prefix", statInfosArray)
    }

    private fun <SV, TV> warmUpPhase(phaseData: PhaseData<SV, TV>) {
        val warmUpStatInfosArray = phase(phaseData, WARM_UP, true)

        if (phaseData.testName != WARM_UP) {
            printWarmUpTimings(phaseData.testName, warmUpStatInfosArray)
        } else {
            printTimings(
                phaseData.testName,
                printOnlyErrors = true,
                statInfoArray = warmUpStatInfosArray
            ) { attempt -> "warm-up #$attempt" }
        }

        warmUpStatInfosArray.filterNotNull().map { it[ERROR_KEY] as? Throwable }.firstOrNull()?.let { throw it }
    }

    private fun <SV, TV> mainPhase(phaseData: PhaseData<SV, TV>): Array<StatInfos> {
        val statInfosArray = phase(phaseData, "")
        statInfosArray.filterNotNull().map { it[ERROR_KEY] as? Throwable }.firstOrNull()?.let {
            printTimings(
                phaseData.testName,
                printOnlyErrors = true,
                statInfoArray = statInfosArray
            )
            throw it
        }
        return statInfosArray
    }

    private fun <SV, TV> phase(phaseData: PhaseData<SV, TV>, phaseName: String, warmup: Boolean = false): Array<StatInfos> {
        val statInfosArray = Array<StatInfos>(phaseData.iterations) { null }
        val testData = TestData<SV, TV>(null, null)

        try {
            for (attempt in 0 until phaseData.iterations) {
                testData.reset()
                triggerGC(attempt)

                val phaseProfiler =
                    createPhaseProfiler(phaseData.testName, phaseName, profilerConfig.copy(warmup = warmup))

                val setUpMillis = measureTimeMillis { phaseData.setUp(testData) }
                val attemptName = "${phaseData.testName} #$attempt"
                logMessage { "$attemptName setup took $setUpMillis ms" }

                val valueMap = HashMap<String, Any>(2 * PerformanceCounter.numberOfCounters + 1)
                statInfosArray[attempt] = valueMap
                try {
                    phaseProfiler.start()
                    valueMap[TEST_KEY] = measureNanoTime {
                        phaseData.test(testData)
                    }

                    PerformanceCounter.report { name, counter, nanos ->
                        valueMap["counter \"$name\": count"] = counter.toLong()
                        valueMap["counter \"$name\": time"] = nanos.nsToMs
                    }

                } catch (t: Throwable) {
                    logMessage(t) { "error at $attemptName" }
                    valueMap[ERROR_KEY] = t
                    break
                } finally {
                    phaseProfiler.stop()
                    try {
                        val tearDownMillis = measureTimeMillis {
                            phaseData.tearDown(testData)
                        }
                        logMessage { "$attemptName tearDown took $tearDownMillis ms" }
                    } catch (t: Throwable) {
                        logMessage(t) { "error at tearDown of $attemptName" }
                        valueMap[ERROR_KEY] = t
                        break
                    } finally {
                        PerformanceCounter.resetAllCounters()
                    }
                }
            }
        } catch (t: Throwable) {
            logMessage(t) { "error at ${phaseData.testName}" }
            TeamCity.testFailed(name, error = t)
        }
        return statInfosArray
    }

    private fun createPhaseProfiler(
        testName: String,
        phaseName: String,
        profilerConfig: ProfilerConfig
    ): PhaseProfiler {
        profilerConfig.name = "$testName${if (phaseName.isEmpty()) "" else "-"+phaseName}"
        profilerConfig.path = pathToResource("profile/${plainname()}").path
        val profilerHandler = if (profilerConfig.enabled && !profilerConfig.warmup)
            ProfilerHandler.getInstance(profilerConfig)
        else
            DummyProfilerHandler

        return if (profilerHandler != DummyProfilerHandler) {
            ActualPhaseProfiler(profilerHandler)
        } else {
            DummyPhaseProfiler
        }
    }

    private fun triggerGC(attempt: Int) {
        if (attempt > 0) {
            val ref = WeakReference(IntArray(32 * 1024))
            while (ref.get() != null) {
                System.gc()
                Thread.sleep(1)
            }
        }
    }

    private fun geomMean(data: List<Long>) = exp(data.fold(0.0, { mul, next -> mul + ln(1.0 * next) }) / data.size)

    override fun close() {
        if (perfTestRawDataMs.isNotEmpty()) {
            val geomMeanMs = geomMean(perfTestRawDataMs.toList()).toLong()
            TeamCity.statValue("$name geomMean", geomMeanMs)
            append(arrayOf("$name geomMean", geomMeanMs, 0))
        }
        statsOutput.flush()
        statsOutput.close()
    }

    companion object {
        const val TEST_KEY = "test"
        const val ERROR_KEY = "error"

        const val WARM_UP = "warm-up"

        inline fun runAndMeasure(note: String, block: () -> Unit) {
            val openProjectMillis = measureTimeMillis {
                block()
            }
            logMessage { "$note took $openProjectMillis ms" }
        }
    }

}

data class PhaseData<SV, TV>(
    val iterations: Int,
    val testName: String,
    val setUp: (TestData<SV, TV>) -> Unit,
    val test: (TestData<SV, TV>) -> Unit,
    val tearDown: (TestData<SV, TV>) -> Unit
)

data class TestData<SV, TV>(var setUpValue: SV?, var value: TV?) {
    fun reset() {
        setUpValue = null
        value = null
    }
}
