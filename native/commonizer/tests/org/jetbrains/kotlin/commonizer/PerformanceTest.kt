/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("MemberVisibilityCanBePrivate")

package org.jetbrains.kotlin.commonizer

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.commonizer.utils.CommonizerMemoryTracker
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.junit.Ignore
import java.io.File
import org.jetbrains.kotlin.commonizer.cli.main as entryPoint

/**
 * This test can be used to make small performance measurements of the core commonizer on
 * occasional basis. This test is not intended for regular execution, e.g. at the CI server.
 *
 * Use [test_distribution_commonization_memory_usage] to check memory consumption over time during
 * the whole commonization process.
 *
 * Use [test_distribution_commonization_throughput] to check the speed of commonization.
 *
 * NOTE: First of all you would need to specify the actual values in [NATIVE_DISTRIBUTION_PATH]
 * and [COMMONIZED_TARGETS] properties!
 *
 * NOTE 2: Don't forget to unmute the test by removing the [Ignore] annotation!
 */
@Ignore
class PerformanceTest : KtUsefulTestCase() {
    private lateinit var outputDir: File

    override fun setUp() {
        outputDir = KtTestUtil.tmpDir(FileUtil.sanitizeFileName(name))
    }

    override fun tearDown() {
        outputDir.deleteRecursively()
    }

    fun test_distribution_commonization_memory_usage() {
        CommonizerMemoryTracker.startTracking("memory-usage", 200, forceGC = true)
        try {
            doTest("memory-usage")
        } finally {
            CommonizerMemoryTracker.stopTracking()
        }
    }

    fun test_distribution_commonization_throughput() {
        doTest("throughput")
    }

    private fun doTest(label: String) {
        val startTimeNanos = System.nanoTime()

        entryPoint(
            arrayOf(
                "native-dist-commonize",
                "-distribution-path", NATIVE_DISTRIBUTION_PATH,
                "-output-path", outputDir.absolutePath,
                "-targets", COMMONIZED_TARGETS.joinToString(","),
                "-output-targets", "(${COMMONIZED_TARGETS.joinToString(",")})"
            )
        )

        val finishTimeNanos = System.nanoTime()
        val elapsedTimeMillis = formatTime((finishTimeNanos - startTimeNanos) / 1000_000L)

        println("$label test finished in $elapsedTimeMillis")
    }

    companion object DefaultCommonizationParameters {
        private const val NATIVE_DISTRIBUTION_PATH = "/path/to/kotlin/native" // Adjust as necessary.
        private val COMMONIZED_TARGETS = listOf("ios_arm64", "ios_x64", "ios_simulator_arm64") // Adjust as necessary.

        private fun formatTime(millis: Long): String = when {
            millis < 1000 -> "${millis}ms"
            millis < 60 * 1000 -> "${millis / 1000}s ${millis % 1000}ms"
            else -> "${millis / (60 * 1000)}m ${millis % (60 * 1000) / 1000}s ${millis % (60 * 1000) % 1000}ms"
        }
    }
}
