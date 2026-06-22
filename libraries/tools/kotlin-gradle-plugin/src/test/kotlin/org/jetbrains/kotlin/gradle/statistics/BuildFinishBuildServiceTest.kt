/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.statistics

import org.gradle.api.logging.LogLevel
import org.jetbrains.kotlin.gradle.plugin.statistics.BuildFinishBuildService
import org.jetbrains.kotlin.gradle.plugin.statistics.BuildFinishBuildService.Companion.findFusFilesByBuildId
import org.jetbrains.kotlin.statistics.metrics.BooleanMetrics
import org.jetbrains.kotlin.statistics.metrics.NumericalMetrics
import org.jetbrains.kotlin.statistics.metrics.StringMetrics
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BuildFinishBuildServiceTest {

    @field:TempDir
    lateinit var tmpFolder: File

    @Test
    fun testFusMetricAggregation() {
        val fusDir = tmpFolder.resolve("fus-dir").also { it.mkdirs() }

        val buildId = "build-id"
        val logger = TestLogger(LogLevel.DEBUG)

        fusDir.resolve("$buildId-a.plugin-profile").writeText(
            """
                unknown-metric=1

                ${NumericalMetrics.COMPILATION_DURATION}=10
                ${StringMetrics.OS_VERSION}=1.0.0-SNAPSHOT
                ${StringMetrics.IDES_INSTALLED}=invalid_version,AS,WC
                ${BooleanMetrics.ENABLED_COMPILER_REFERENCE_INDEX}=true
                ${BooleanMetrics.KOTLIN_PROGRESSIVE_MODE}=true
 
                BUILD FINISHED
            """.trimIndent()
        )
        fusDir.resolve("$buildId-b.plugin-profile").writeText(
            """
                unknown-metric=1
                wrong format
                ${NumericalMetrics.COMPILATION_DURATION}=10
                ${StringMetrics.OS_VERSION}=invalid_version
                ${BooleanMetrics.ENABLED_COMPILER_REFERENCE_INDEX}=invalid
                ${BooleanMetrics.KOTLIN_PROGRESSIVE_MODE}=invalid
                BUILD FINISHED
            """.trimIndent()
        )
        fusDir.resolve("another-id.plugin-profile").writeText(
            """
                ${BooleanMetrics.TESTS_EXECUTED}=true
                BUILD FINISHED
            """.trimIndent()
        )
        fusDir.resolve("$buildId.c.kotlin-profile").writeText(
            """
                ${BooleanMetrics.BUILD_SCAN_BUILD_REPORT}=true
                ${StringMetrics.OS_VERSION}=2.0.0-SNAPSHOT
                ${StringMetrics.IDES_INSTALLED}=IU
                ${BooleanMetrics.ENABLED_COMPILER_REFERENCE_INDEX}=false
                ${BooleanMetrics.KOTLIN_PROGRESSIVE_MODE}=false
                BUILD FINISHED
            """.trimIndent()
        )


        val errorMessages = BuildFinishBuildService.collectAllFusReportsIntoOne(buildId, fusDir, "test version", logger)
        assertTrue("No error messages expected") { errorMessages.isEmpty() }

        assertTrue("finish-profile file should be created after build finish") {
            fusDir.resolve("$buildId.finish-profile").exists()
        }

        val fusProfileFile = fusDir.resolve("$buildId.profile")
        assertTrue("old profile file should be created after build finish") {
            fusProfileFile.exists()
        }

        val profileContent = fusProfileFile.readText()
        assertContains(profileContent, "${NumericalMetrics.COMPILATION_DURATION}=20", message = "Profile file should contain valid metrics")
        assertContains(profileContent, "${StringMetrics.OS_VERSION}=2.0.0-snapshot")
        assertContains(profileContent, "${StringMetrics.IDES_INSTALLED}=AS;IU;WC;UNEXPECTED-VALUE")
        assertContains(profileContent, "${BooleanMetrics.ENABLED_COMPILER_REFERENCE_INDEX}=true")
        assertContains(profileContent, "${BooleanMetrics.KOTLIN_PROGRESSIVE_MODE}=false")
        assertTrue("Profile file should not contain metrics from another build") {
            !profileContent.contains(BooleanMetrics.TESTS_EXECUTED.name)
        }

    }

    @Test
    fun testFindFusFilesByBuildId() {
        val fusReportDirectory = tmpFolder.resolve("fus").also { it.mkdirs() }
        val buildId = "build123"
        val modificationTime = System.currentTimeMillis()
        fusReportDirectory.resolve("$buildId.2.plugin-profile").also {
            it.createNewFile()
            it.setLastModified(modificationTime)
        }
        fusReportDirectory.resolve("$buildId.3.kotlin-profile").also {
            it.createNewFile()
            it.setLastModified(modificationTime)
        }
        fusReportDirectory.resolve("another-$buildId.1.plugin-profile").also {
            it.createNewFile()
        }
        fusReportDirectory.resolve("$buildId.1.wrong-extension").also {
            it.createNewFile()
        }
        fusReportDirectory.resolve("$buildId.1.plugin-profile").also {
            it.createNewFile()
            it.setLastModified(modificationTime + 1)
        }

        val fusFiles = fusReportDirectory.findFusFilesByBuildId(buildId)?.map { it.name }
        assertEquals(listOf("$buildId.2.plugin-profile", "$buildId.3.kotlin-profile", "$buildId.1.plugin-profile"), fusFiles)
    }

}
