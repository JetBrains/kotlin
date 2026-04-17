/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.statistics

import org.jetbrains.kotlin.statistics.BuildSessionLogger
import org.jetbrains.kotlin.statistics.BuildSessionLogger.Companion.FUS_KOTLIN_FILE_NAME_SUFFIX
import org.jetbrains.kotlin.statistics.fileloggers.MetricsContainer
import org.jetbrains.kotlin.statistics.fileloggers.MetricsContainer.Companion.addMetricFromFusKotlinProfileFile
import org.jetbrains.kotlin.statistics.metrics.BooleanMetrics
import org.jetbrains.kotlin.statistics.metrics.NumericalMetrics
import org.jetbrains.kotlin.statistics.metrics.StringAnonymizationPolicy
import org.jetbrains.kotlin.statistics.metrics.StringMetrics
import org.jetbrains.kotlin.statistics.metrics.StringOverridePolicy
import java.io.File
import java.nio.file.Files
import java.util.*
import kotlin.collections.ArrayList
import kotlin.test.*

class BuildSessionLoggerTest {

    private lateinit var statsFolder: File

    private fun statFilesCount() = statsFolder.listFiles()?.size ?: 0

    @BeforeTest
    fun prepareFolder() {
        statsFolder = Files.createTempDirectory("kotlin-stats").toFile()
    }

    @AfterTest
    fun cleanFolder() {
        statsFolder.deleteRecursively()
    }

    @Test
    fun createSeveralTrackingFiles() {
        val logger1 = BuildSessionLogger(statsFolder)
        val logger2 = BuildSessionLogger(statsFolder)

        //check that starting session creates a new log file
        assertEquals(false, logger1.isBuildSessionStarted())
        logger1.startBuildSession("build_1")

        // check that file locks work properly and do not use the same file
        logger2.startBuildSession("build_2")

        assertEquals(true, logger1.isBuildSessionStarted())

        logger1.finishBuildSession()
        // created single folder with kotlin statistics
        assertEquals(1, statFilesCount())

        logger2.finishBuildSession()
        assertEquals(2, statFilesCount())

        statsFolder.listFiles()?.forEach { file ->
            assertTrue(
                file.name.matches(BuildSessionLogger.STATISTICS_FILE_NAME_PATTERN),
                "Check that file name \'${file.name}\' matches pattern \'${BuildSessionLogger.STATISTICS_FILE_NAME_PATTERN}\'"
            )
        }
    }

    @Test
    fun testDeleteExtraFiles() {
        val maxFiles = 100
        val buildId = UUID.randomUUID().toString()
        val logger = BuildSessionLogger(statsFolder, maxFiles)
        logger.startBuildSession(buildId)
        logger.finishBuildSession()

        assertEquals(1, statFilesCount(), "stat file was not created")

        for (i in 1..200) {
            File(statsFolder, "$i").createNewFile()
            File(statsFolder, "${UUID.randomUUID()}$FUS_KOTLIN_FILE_NAME_SUFFIX").createNewFile()
        }

        logger.startBuildSession(buildId)
        logger.finishBuildSession()

        //the first created file for build finished should be deleted as an old file
        assertEquals(
            1,
            statsFolder.listFiles()?.count { it.readText().startsWith("Build: $buildId") },
            "Could not find expected files for '$buildId' build"
        )

        // files not matching the pattern should not be affected
        assertEquals(
            200,
            statsFolder.listFiles()?.count { !it.name.matches(BuildSessionLogger.STATISTICS_FILE_NAME_PATTERN) },
            "Some files which should not be affected, were removed"
        )

        assertEquals(
            maxFiles,
            statsFolder.listFiles()?.count { it.name.matches(BuildSessionLogger.STATISTICS_FILE_NAME_PATTERN) },
            "Some files which should not be affected, were removed"
        )
    }

    @Test
    fun testReadWriteMetrics() {
        val buildId = "test"
        val logger = BuildSessionLogger(statsFolder)
        logger.report(StringMetrics.KOTLIN_COMPILER_VERSION, "1.2.3.4-snapshot")

        logger.startBuildSession(buildId)
        val reportFile = statsFolder.resolve(UUID.randomUUID().toString() + FUS_KOTLIN_FILE_NAME_SUFFIX)

        reportFile.createNewFile()
        //FUS metrics file should contain "BUILD FINISHED", otherwise it will be skipped as invalid
        reportFile.appendText(
            """
            ${StringMetrics.USE_FIR.name}=true
            BUILD FINISHED
           
            """.trimIndent()
        )

        val invalidReportFile = statsFolder.resolve(UUID.randomUUID().toString() + FUS_KOTLIN_FILE_NAME_SUFFIX)

        invalidReportFile.appendText(
            """
            5tetetr=true
            
            """.trimIndent()
        )

        logger.finishBuildSession()
        assertEquals(3, statFilesCount(), "Three files should be created: one from logger build finish, reportFile and invalidReportFile")

        val statFiles = statsFolder.listFiles() ?: fail("Could not find stat file")
        statFiles.forEach { it.appendBytes("break format of the file".toByteArray()) } //this line should be filtered by MetricsContainer.readFromFile

        logger.startBuildSession(buildId)
        logger.finishBuildSession()

        val metrics = ArrayList<MetricsContainer>()
        statsFolder.listFiles()?.forEach { file ->
            MetricsContainer.readFromFile(file) {
                metrics.add(it)
            }
        }

        assertEquals(3, metrics.size, "only invalidReportFile file should be filtered")
        assertEquals(
            1,
            metrics.filter { it.getMetric(StringMetrics.USE_FIR)?.getValue() == "true" }.size,
            "USE_FIR metric should be red from reportFile"
        )

        assertEquals(
            1,
            metrics.filter { it.getMetric(StringMetrics.KOTLIN_COMPILER_VERSION)?.getValue() == "1.2.3" }.size,
            "KOTLIN_COMPILER_VERSION metric should be red from logger build finished file"
        )

        assertEquals(
            0,
            metrics.filter { it.getMetric(StringMetrics.USE_OLD_BACKEND)?.getValue() != null }.size,
            "USE_OLD_BACKEND metric should be red"
        )
    }

    @Test
    fun testSaveAndReadAllMetrics() {
        val logger = BuildSessionLogger(statsFolder)
        logger.startBuildSession("test")
        for (metric in StringMetrics.entries) {
            when (val anonymization = metric.anonymization) {
                is StringAnonymizationPolicy.ComponentVersionAnonymizer -> {
                    logger.report(metric, "1.2.3")
                    logger.report(metric, "1.2.3-SNAPSHOT")
                }
                is StringAnonymizationPolicy.AllowedListAnonymizer -> {
                    anonymization.allowedValues.sorted().forEach {
                        logger.report(metric, it)
                    }
                }
                is StringAnonymizationPolicy.RegexControlled -> logger.report(metric, metric.name)
                else -> {
                    logger.report(metric, "value")
                    logger.report(metric, metric.name)
                }
            }
        }
        for (metric in BooleanMetrics.entries) {
            logger.report(metric, true)
        }
        for (metric in NumericalMetrics.entries) {
            logger.report(metric, System.currentTimeMillis())
        }

        logger.finishBuildSession() // create kotlin-profile fus file with comma-separated values

        // read metrics from kotlin-profile file in old format with semicolon-separated values
        val metricContainer = MetricsContainer.createMetricsContainerForProfileFile()

        metricContainer.addMetricFromFusKotlinProfileFile(statsFolder.listFiles()?.single() ?: fail("Could not find stat file"))

        for (metric in StringMetrics.entries) {
            val metricValue = metricContainer.getMetric(metric)?.getValue() ?: fail("Could not find metric ${metric.name}")

            when (val anonymization = metric.anonymization) {
                is StringAnonymizationPolicy.ComponentVersionAnonymizer -> validateMetricValueBasedOnOverrideRule(metric, listOf("1.2.3", "1.2.3-snapshot"), metricValue)
                is StringAnonymizationPolicy.AllowedListAnonymizer -> validateMetricValueBasedOnOverrideRule(metric, anonymization.allowedValues.sorted(), metricValue)
                is StringAnonymizationPolicy.RegexControlled -> assertMetricValueEquals(metric, metric.name, metricValue)
                else -> validateMetricValueBasedOnOverrideRule(metric, listOf("value", metric.name), metricValue)
            }
        }

        for (metric in BooleanMetrics.entries) {
            assertTrue(metricContainer.getMetric(metric)?.getValue() != null, "Could not find metric ${metric.name}")
        }

        for (metric in NumericalMetrics.entries) {
            assertNotNull(metricContainer.getMetric(metric), "Could not find metric ${metric.name}")
        }
    }

    private fun validateMetricValueBasedOnOverrideRule(metric: StringMetrics, possibleExpectedValues: List<String>, actualValue: String) {
        when (metric.type) {
            StringOverridePolicy.OVERRIDE -> assertMetricValueEquals(metric, possibleExpectedValues.last(), actualValue)
            StringOverridePolicy.OVERRIDE_VERSION_IF_NOT_SET -> assertMetricValueEquals(metric, possibleExpectedValues.first(), actualValue)
            StringOverridePolicy.CONCAT -> assertMetricValueEquals(metric, possibleExpectedValues.joinToString(";"), actualValue)
        }
    }

    private fun assertMetricValueEquals(metric: StringMetrics, expectedValue: String, actualValue: String) {
        assertEquals(
            expectedValue,
            actualValue,
            "Metric ${metric.name} contains unexpected value: expected $expectedValue, but found $actualValue"
        )
    }

    @Test
    fun testWeight() {
        val logger = BuildSessionLogger(statsFolder)
        logger.startBuildSession("build_1")

        logger.report(NumericalMetrics.ANALYSIS_LINES_PER_SECOND, 10, null, 9)
        logger.report(NumericalMetrics.ANALYSIS_LINES_PER_SECOND, 100, null, 1)

        logger.finishBuildSession()
        MetricsContainer.readFromFile(statsFolder.listFiles()?.single() ?: fail("Could not find stat file")) {
            assertEquals(19L, it.getMetric(NumericalMetrics.ANALYSIS_LINES_PER_SECOND)?.getValue())
        }
    }
}
