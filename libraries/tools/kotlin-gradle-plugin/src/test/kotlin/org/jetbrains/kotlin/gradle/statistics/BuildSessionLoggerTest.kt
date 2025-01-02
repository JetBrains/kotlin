/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.statistics

import org.jetbrains.kotlin.statistics.BuildSessionLogger
import org.jetbrains.kotlin.statistics.fileloggers.MetricsContainer
import org.jetbrains.kotlin.statistics.metrics.BooleanMetrics
import org.jetbrains.kotlin.statistics.metrics.NumericalMetrics
import org.jetbrains.kotlin.statistics.metrics.StringMetrics
import org.jetbrains.kotlin.test.MuteableTestRule
import org.junit.After
import org.junit.Before
import org.junit.Rule
import java.io.File
import java.nio.file.Files
import java.util.*
import kotlin.collections.ArrayList
import kotlin.test.*

class BuildSessionLoggerTest {
    @get:Rule val muteableTestRule = MuteableTestRule()

    private lateinit var rootFolder: File

    private fun statFilesCount() = rootFolder.listFiles()?.single()?.listFiles()?.size ?: 0

    @Before
    fun prepareFolder() {
        rootFolder = Files.createTempFile("kotlin-stats", "").toFile()
        rootFolder.delete()
        rootFolder.mkdirs()
    }

    @After
    fun cleanFolder() {
        rootFolder.deleteRecursively()
    }

    @Test
    fun createSeveralTrackingFiles() {
        val logger1 = BuildSessionLogger(rootFolder)
        val logger2 = BuildSessionLogger(rootFolder)

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

        rootFolder.listFiles()?.first()?.listFiles()?.forEach { file ->
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
        val logger = BuildSessionLogger(rootFolder, maxFiles)
        logger.startBuildSession(buildId)
        logger.finishBuildSession()
        assertEquals(1, statFilesCount())

        val statsFolder = rootFolder.listFiles()?.single() ?: fail("${rootFolder.absolutePath} was not created")
        assertEquals(1, statsFolder.listFiles()?.size, "stat file was not created")

        for (i in 1..200) {
            File(statsFolder, "$i").createNewFile()
            File(statsFolder, "${UUID.randomUUID()}.profile").createNewFile()
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
        val logger = BuildSessionLogger(rootFolder)
        logger.report(StringMetrics.KOTLIN_COMPILER_VERSION, "1.2.3.4-snapshot")

        logger.startBuildSession(buildId)
        val reportFile = rootFolder.resolve(BuildSessionLogger.Companion.STATISTICS_FOLDER_NAME)
            .resolve(UUID.randomUUID().toString() + BuildSessionLogger.Companion.PROFILE_FILE_NAME_SUFFIX)

        reportFile.createNewFile()
        //FUS metrics file should contain "BUILD FINISHED", otherwise it will be skipped as invalid
        reportFile.appendText(
            """
            ${StringMetrics.USE_FIR.name}=true
            BUILD FINISHED
           
            """.trimIndent()
        )

        val invalidReportFile = rootFolder.resolve(BuildSessionLogger.Companion.STATISTICS_FOLDER_NAME)
            .resolve(UUID.randomUUID().toString() + BuildSessionLogger.Companion.PROFILE_FILE_NAME_SUFFIX)

        invalidReportFile.appendText(
            """
            ${StringMetrics.USE_OLD_BACKEND.name}=true
            
            """.trimIndent()
        )

        logger.finishBuildSession()
        assertEquals(3, statFilesCount(), "Three files should be created: one from logger build finish, reportFile and invalidReportFile")

        val statFiles = rootFolder.listFiles()?.single()?.listFiles() ?: fail("Could not find stat file")
        statFiles.forEach { it.appendBytes("break format of the file".toByteArray()) } //this line should be filtered by MetricsContainer.readFromFile

        logger.startBuildSession(buildId)
        logger.finishBuildSession()

        val metrics = ArrayList<MetricsContainer>()
        rootFolder.listFiles()?.single()?.listFiles()?.forEach { file ->
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
        val logger = BuildSessionLogger(rootFolder)
        logger.startBuildSession("test")
        for (metric in StringMetrics.values()) {
            logger.report(metric, "value")
            logger.report(metric, metric.name)
        }
        for (metric in BooleanMetrics.values()) {
            logger.report(metric, true)
        }
        for (metric in NumericalMetrics.values()) {
            logger.report(metric, System.currentTimeMillis())
        }
        logger.finishBuildSession()

        MetricsContainer.readFromFile(rootFolder.listFiles()?.single()?.listFiles()?.single() ?: fail("Could not find stat file")) {
            for (metric in StringMetrics.values()) {
                assertNotNull(it.getMetric(metric), "Could not find metric ${metric.name}")
            }
            for (metric in BooleanMetrics.values()) {
                assertTrue(it.getMetric(metric)?.getValue() != null, "Could not find metric ${metric.name}")
            }
            for (metric in NumericalMetrics.values()) {
                assertNotNull(it.getMetric(metric), "Could not find metric ${metric.name}")
            }
        }
    }

    @Test
    fun testWeight() {
        val logger = BuildSessionLogger(rootFolder)
        logger.startBuildSession("build_1")

        logger.report(NumericalMetrics.ANALYSIS_LINES_PER_SECOND, 10, null, 9)
        logger.report(NumericalMetrics.ANALYSIS_LINES_PER_SECOND, 100, null, 1)

        logger.finishBuildSession()
        MetricsContainer.readFromFile(rootFolder.listFiles()?.single()?.listFiles()?.single() ?: fail("Could not find stat file")) {
            assertEquals(19L, it.getMetric(NumericalMetrics.ANALYSIS_LINES_PER_SECOND)?.getValue())
        }
    }
}
