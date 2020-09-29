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
import java.lang.ref.WeakReference
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.*
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis
import kotlin.test.assertEquals

typealias StatInfos = Map<String, Any>?

class Stats(
    val name: String = "",
    private val profilerConfig: ProfilerConfig = ProfilerConfig(),
    private val header: Array<String> = arrayOf("Name", "ValueMS", "StdDev"),
    private val acceptanceStabilityLevel: Int = 25
) : AutoCloseable {

    private val perfTestRawDataMs = mutableListOf<Long>()

    private val metrics = mutableListOf<Metric>()

    init {
        PerformanceCounter.setTimeCounterEnabled(true)
    }

    private fun calcAndProcessMetrics(id: String, statInfosArray: Array<StatInfos>, rawMetricChildren: MutableList<Metric>) {
        val timingsMs = toTimingsMs(statInfosArray)

        val calcMean = calcMean(timingsMs)

        val metricChildren = mutableListOf<Metric>()
        val metric = Metric(id, calcMean.mean.toLong(), measurementError = calcMean.stdDev.toLong(), children = metricChildren)
        metrics.add(metric)

        metricChildren.add(
            Metric(
                "", calcMean.mean.toLong(),
                measurementError = calcMean.stdDev.toLong(),
                childrenName = "raw_metrics", children = rawMetricChildren
            )
        )
        metricChildren.add(Metric("mean", calcMean.mean.toLong()))
        // keep geomMean for bwc
        metricChildren.add(Metric(GEOM_MEAN, calcMean.geomMean.toLong()))
        metricChildren.add(Metric("std_dev", calcMean.stdDev.toLong()))

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
                val metricShortName = if (perfCounterName.endsWith(": time")) perfCounterName.removeSuffix(": time") else perfCounterName

                metricChildren.add(Metric(": $metricShortName", mean))

                TeamCity.test(shortName, durationMs = mean) {}
            }

        perfTestRawDataMs.addAll(timingsMs.toList())
        metric.writeTeamCityStats(name)
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
            val metricChildren = mutableListOf<Metric>()
            warmUpPhase(warmPhaseData, metricChildren)
            val statInfoArray = mainPhase(mainPhaseData, metricChildren)

            assertEquals(iterations, statInfoArray.size)
            if (testName != WARM_UP) {
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

                    TeamCity.test(stabilityName, errorDetails = error, includeStats = false) {
                        metricChildren.add(Metric("stability", stabilityPercentage))
                    }
                }

                processTimings(testName, statInfoArray, metricChildren)
            } else {
                convertStatInfoIntoMetrics(testName, printOnlyErrors = true, statInfoArray = statInfoArray, metricChildren = metricChildren)
            }
        }

        if (testName != WARM_UP) {
            TeamCity.suite(testName, block)
        } else {
            block()
        }
    }

    private fun convertStatInfoIntoMetrics(
        prefix: String,
        statInfoArray: Array<StatInfos>,
        printOnlyErrors: Boolean = false,
        metricChildren: MutableList<Metric>,
        attemptFn: (Int) -> String = { attempt -> "#$attempt" }
    ) {
        for (statInfoIndex in statInfoArray.withIndex()) {
            val attempt = statInfoIndex.index
            val statInfo = statInfoIndex.value ?: continue
            val attemptString = attemptFn(attempt)
            val s = "$prefix $attemptString"
            val n = "$name: $s"
            val childrenMetrics = mutableListOf<Metric>()

            val t = statInfo[ERROR_KEY] as? Throwable
            if (t != null) {
                TeamCity.test(n, errors = listOf(t)) {}
            } else if (!printOnlyErrors) {
                val durationMs = (statInfo[TEST_KEY] as Long).nsToMs
                TeamCity.test(n, durationMs = durationMs) {
                    for ((k, v) in statInfo) {
                        if (k == TEST_KEY) continue
                        (v as? Number)?.let {
                            childrenMetrics.add(Metric(k, v))
                            //TeamCity.metadata(n, k, it)
                        }
                    }
                }
                metricChildren.add(Metric(attemptString, durationMs, children = childrenMetrics))
            }
        }
    }

    fun printWarmUpTimings(
        prefix: String,
        warmUpStatInfosArray: Array<StatInfos>,
        metricChildren: MutableList<Metric>
    ) = convertStatInfoIntoMetrics(prefix, warmUpStatInfosArray, metricChildren = metricChildren) { attempt -> "warm-up #$attempt" }

    fun processTimings(
        prefix: String,
        statInfosArray: Array<StatInfos>,
        metricChildren: MutableList<Metric>
    ) {
        convertStatInfoIntoMetrics(prefix, statInfosArray, metricChildren = metricChildren)
        calcAndProcessMetrics(prefix, statInfosArray, metricChildren)
    }

    private fun <SV, TV> warmUpPhase(phaseData: PhaseData<SV, TV>, metricChildren: MutableList<Metric>) {
        val warmUpStatInfosArray = phase(phaseData, WARM_UP, true)

        if (phaseData.testName != WARM_UP) {
            printWarmUpTimings(phaseData.testName, warmUpStatInfosArray, metricChildren)
        } else {
            convertStatInfoIntoMetrics(
                phaseData.testName,
                printOnlyErrors = true,
                statInfoArray = warmUpStatInfosArray,
                metricChildren = metricChildren
            ) { attempt -> "warm-up #$attempt" }
        }

        warmUpStatInfosArray.filterNotNull().map { it[ERROR_KEY] as? Throwable }.firstOrNull()?.let { throw it }
    }

    private fun <SV, TV> mainPhase(phaseData: PhaseData<SV, TV>, metricChildren: MutableList<Metric>): Array<StatInfos> {
        val statInfosArray = phase(phaseData, "")
        statInfosArray.filterNotNull().map { it[ERROR_KEY] as? Throwable }.firstOrNull()?.let {
            convertStatInfoIntoMetrics(
                phaseData.testName,
                printOnlyErrors = true,
                statInfoArray = statInfosArray,
                metricChildren = metricChildren
            )
            throw it
        }
        return statInfosArray
    }

    private fun <SV, TV> phase(phaseData: PhaseData<SV, TV>, phaseName: String, warmup: Boolean = false): Array<StatInfos> {
        val statInfosArray = Array<StatInfos>(phaseData.iterations) { null }
        val testData = TestData<SV, TV>(null, null)

        try {
            val phaseProfiler =
                createPhaseProfiler(phaseData.testName, phaseName, profilerConfig.copy(warmup = warmup))

            for (attempt in 0 until phaseData.iterations) {
                testData.reset()
                triggerGC(attempt)

                val setUpMillis = measureTimeMillis {
                    //phaseData.setUp(testData)
                }
                val attemptName = "${phaseData.testName} #$attempt"
                logMessage { "$attemptName setup took $setUpMillis ms" }

                val valueMap = HashMap<String, Any>(2 * PerformanceCounter.numberOfCounters + 1)
                statInfosArray[attempt] = valueMap
                try {
                    phaseProfiler.start()
                    valueMap[TEST_KEY] = measureNanoTime {
                        //phaseData.test(testData)
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
                            //phaseData.tearDown(testData)
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
        profilerConfig.path = pathToResource("profile/${plainname(name)}")
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
        flush()
    }

    fun flush() {
        val children = metrics.toMutableList()

        val properties = mutableMapOf<String, Any>()
        properties[BENCHMARK] = name

        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
        simpleDateFormat.timeZone = TimeZone.getTimeZone("UTC")
//        properties["build_timestamp"] = simpleDateFormat.format(Date())
//        properties["build_id"] = 87015694
//        properties["build_branch"] = "rr/perf/json-output"
//        properties["agent_name"] = "kotlin-linux-perf-unit879"

        System.getenv("TEAMCITY_BUILD_PROPERTIES_FILE")?.let { teamcityConfig ->
            val buildProperties = Properties()
            buildProperties.load(FileInputStream(teamcityConfig))

            properties["build.timestamp"] = simpleDateFormat.format(Date())
            for ((name, key) in
            mapOf(
                "build_id" to "teamcity.build.id",
                "build_branch" to "teamcity.build.branch",
                "agent_name" to "agent.name",
            )) {
                val property = buildProperties.getProperty(key)
                properties[name] = if (name == "build_id") property.toLong() else property
            }
        }
        if (perfTestRawDataMs.isNotEmpty()) {
            val geomMeanMs = geomMean(perfTestRawDataMs.toList()).toLong()
            Metric(GEOM_MEAN, geomMeanMs).writeTeamCityStats(name)
            properties[GEOM_MEAN] = geomMeanMs
        }

        val metric = Metric(name, null, children = children, properties = properties)

        metric.writeJson()
        metrics.writeCSV(name, header)
    }

    companion object {
        const val TEST_KEY = "test"
        const val ERROR_KEY = "error"

        const val WARM_UP = "warm-up"
        const val GEOM_MEAN = "geomMean"
        const val BENCHMARK = "benchmark"

        inline fun runAndMeasure(note: String, block: () -> Unit) {
            val openProjectMillis = measureTimeMillis {
                block()
            }
            logMessage { "$note took $openProjectMillis ms" }
        }
    }

}

data class Metric(
    val name: String,
    val value: Number?,
    val measurementError: Number? = null,
    val childrenName: String = "metrics",
    val children: MutableList<Metric> = mutableListOf(),
    val properties: Map<String, Any>? = null
)

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
