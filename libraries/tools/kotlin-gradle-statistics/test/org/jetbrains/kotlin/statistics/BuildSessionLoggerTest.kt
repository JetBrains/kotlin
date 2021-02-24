/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.statistics

import org.jetbrains.kotlin.statistics.BuildSessionLogger.Companion.STATISTICS_FILE_NAME_PATTERN
import org.jetbrains.kotlin.statistics.BuildSessionLogger.Companion.listProfileFiles
import org.jetbrains.kotlin.statistics.fileloggers.MetricsContainer
import org.jetbrains.kotlin.statistics.metrics.BooleanMetrics
import org.jetbrains.kotlin.statistics.metrics.NumericalMetrics
import org.jetbrains.kotlin.statistics.metrics.StringMetrics
import org.junit.After
import org.junit.Before
import java.io.File
import java.lang.IllegalStateException
import java.nio.file.Files
import kotlin.test.*

class BuildSessionLoggerTest {

    private lateinit var rootFolder: File

    private fun statFilesCount() = rootFolder.listFiles().single().listFiles().size

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
        logger1.startBuildSession(0, 0)

        // created single folder with kotlin statistics
        assertEquals(1, statFilesCount())

        // check that file locks work properly and do not use the same file
        logger2.startBuildSession(0, 0)
        assertEquals(2, statFilesCount())

        assertEquals(true, logger1.isBuildSessionStarted())

        logger1.finishBuildSession("", null)
        logger2.finishBuildSession("", null)

        rootFolder.listFiles().first().listFiles().forEach { file ->
            assertTrue(
                file.name.matches(STATISTICS_FILE_NAME_PATTERN.toRegex()),
                "Check that file name ${file.name} matches pattern $STATISTICS_FILE_NAME_PATTERN"
            )
        }
    }

    @Test
    fun testSizeLimitation() {
        val maxFileSize = 10_000
        val logger = BuildSessionLogger(rootFolder, 100, maxFileSize.toLong())
        logger.startBuildSession(0, null)
        logger.finishBuildSession("", null)

        assertEquals(1, statFilesCount())

        val file2edit = rootFolder.listFiles().first().listFiles().first()

        logger.startBuildSession(0, 0)
        logger.finishBuildSession("", null)
        //new file should not be created
        assertEquals(1, statFilesCount())

        file2edit.appendBytes(ByteArray(maxFileSize))

        logger.startBuildSession(0, 0)
        logger.finishBuildSession("", null)

        // a new file should be created as maximal file size
        assertEquals(2, statFilesCount())
    }

    @Test
    fun testDeleteExtraFiles() {
        val maxFiles = 100
        val logger = BuildSessionLogger(rootFolder, maxFiles)
        logger.startBuildSession(0, null)
        logger.finishBuildSession("", null)
        assertEquals(1, statFilesCount())

        val statsFolder = rootFolder.listFiles().single()
        val singleStatFile = statsFolder.listFiles().single()

        for (i in 1..200) {
            File(statsFolder, "$i").createNewFile()
            val formattedIndex = "%04d".format(i)
            File(statsFolder, "$formattedIndex-12-30-12-59-59-123.profile").createNewFile()
        }

        logger.startBuildSession(0, 0)
        logger.finishBuildSession("", null)

        assertTrue(
            statsFolder.listFiles().filter { it.name == singleStatFile.name }.count() == 1,
            "Could not find expected file ${singleStatFile.name}"
        )

        // files not matching the pattern should not be affected
        assertEquals(
            200,
            statsFolder.listFiles().filter { !it.name.matches(STATISTICS_FILE_NAME_PATTERN.toRegex()) }.count(),
            "Some files which should not be affected, were removed"
        )

        assertEquals(
            maxFiles,
            statsFolder.listFiles().filter { it.name.matches(STATISTICS_FILE_NAME_PATTERN.toRegex()) }.count(),
            "Some files which should not be affected, were removed"
        )
        assertEquals(
            statsFolder.listFiles().filter { it.name.matches(STATISTICS_FILE_NAME_PATTERN.toRegex()) }.sorted(),
            listProfileFiles(statsFolder)
        )
    }

    @Test
    fun testReadWriteMetrics() {
        val logger = BuildSessionLogger(rootFolder)
        logger.report(StringMetrics.KOTLIN_COMPILER_VERSION, "1.2.3.4-snapshot")

        val startTime = System.currentTimeMillis() - 1001
        logger.startBuildSession(1, startTime)
        logger.finishBuildSession("Build", null)
        assertEquals(1, statFilesCount())

        val statFile = rootFolder.listFiles().single().listFiles().single()
        statFile.appendBytes("break format of the file".toByteArray())

        logger.startBuildSession(1, startTime)

        // the file should be locked
        try {
            MetricsContainer.readFromFile(statFile) {
                fail("No metrics should be read due to locked file")
            }
            fail("Method should have failed")
        } catch (e: IllegalStateException) {
            //all right
        }


        logger.finishBuildSession("", null)

        val metrics = ArrayList<MetricsContainer>()
        MetricsContainer.readFromFile(statFile) {
            metrics.add(it)
        }

        assertEquals(2, metrics.size, "Invalid number of MerticContainers was read")
        assertEquals(
            "1.2.3",
            metrics[0].getMetric(StringMetrics.KOTLIN_COMPILER_VERSION)?.getValue()
        )

        assertEquals(
            null,
            metrics[1].getMetric(StringMetrics.KOTLIN_COMPILER_VERSION)?.getValue()
        )

        val buildDuration = metrics[0].getMetric(NumericalMetrics.GRADLE_BUILD_DURATION)?.getValue() ?: 0L
        assertTrue(buildDuration > 1000, "It was expected that build duration is > 1000, but got $buildDuration")
    }

    @Test
    fun testSaveAndReadAllMetrics() {
        val logger = BuildSessionLogger(rootFolder)
        logger.startBuildSession(1, 1)
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
        logger.finishBuildSession("Build", null)

        MetricsContainer.readFromFile(rootFolder.listFiles().single().listFiles().single()) {
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
}
