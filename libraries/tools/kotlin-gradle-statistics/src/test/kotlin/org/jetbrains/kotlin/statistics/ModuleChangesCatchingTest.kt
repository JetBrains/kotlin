/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.statistics

import org.jetbrains.kotlin.statistics.metrics.BooleanMetrics
import org.jetbrains.kotlin.statistics.metrics.NumericalMetrics
import org.jetbrains.kotlin.statistics.metrics.StringMetrics
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals

private const val SOURCE_CODE_RELATIVE_PATH =
    "libraries/tools/kotlin-gradle-statistics/src/main/kotlin/org/jetbrains/kotlin/statistics/metrics"
private const val BOOLEAN_METRICS_RELATIVE_PATH = "$SOURCE_CODE_RELATIVE_PATH/BooleanMetrics.kt"
private const val STRING_METRICS_RELATIVE_PATH = "$SOURCE_CODE_RELATIVE_PATH/StringMetrics.kt"
private const val NUMERICAL_METRICS_RELATIVE_PATH = "$SOURCE_CODE_RELATIVE_PATH/NumericalMetrics.kt"

private val STRING_METRICS_EXPECTED_VERSION_AND_HASH = Pair(1, "90347332db2ce54b51e7daa64595371e")
private val BOOLEAN_METRICS_EXPECTED_VERSION_AND_HASH = Pair(3, "3c8c4ca636adee168e99862244a22520")
private val NUMERICAL_METRICS_EXPECTED_VERSION_AND_HASH = Pair(2, "d8c1a1f4fb7227fbe8247320bf3370ca")
private val SOURCE_FOLDER_EXPECTED_VERSION_AND_HASH =
    Pair(
        STRING_METRICS_EXPECTED_VERSION_AND_HASH.first +
                BOOLEAN_METRICS_EXPECTED_VERSION_AND_HASH.first +
                NUMERICAL_METRICS_EXPECTED_VERSION_AND_HASH.first,
        "8b845ce616f5bdb9b885ededcdfd812d"
    )
private const val HASH_ALG = "MD5"

/**
 * This class searches for all the changes in kotlin-gradle-statistics
 * and if there is such changes then it requires to upgrade version of connected metrics.
 */
class ModuleChangesCatchingTest {

    /**
     * Test checks for that the version of [StringMetrics] was increased after changes in this file
     */
    @Test
    fun testChecksCorrectChangingStringMetricsVersion() {
        val actualStringMetricsVersionAndHash =
            Pair(StringMetrics.VERSION, calculateFileChecksum(STRING_METRICS_RELATIVE_PATH))
        assertEquals(
            STRING_METRICS_EXPECTED_VERSION_AND_HASH, actualStringMetricsVersionAndHash,
            "Hash of ${StringMetrics::class.qualifiedName} has been changed, please increase VERSION value. " +
                    "Also you need to update hash and version in this test class."
        )
    }

    /**
     * Test checks for that the version of [BooleanMetrics] was increased after changes in this file
     */
    @Test
    fun testChecksCorrectChangingBooleanMetricsVersion() {
        val actualBooleanMetricsVersionAndHash =
            Pair(BooleanMetrics.VERSION, calculateFileChecksum(BOOLEAN_METRICS_RELATIVE_PATH))
        assertEquals(
            BOOLEAN_METRICS_EXPECTED_VERSION_AND_HASH, actualBooleanMetricsVersionAndHash,
            "Hash of ${BooleanMetrics::class.qualifiedName} has been changed, please increase VERSION value. " +
                    "Also you need to update hash and version in this test class."
        )
    }

    /**
     * Test checks for that the version of [NumericalMetrics] was increased after changes in this file
     */
    @Test
    fun testChecksCorrectChangingNumericalMetricsVersion() {
        val actualNumericalMetricsVersionAndHash =
            Pair(NumericalMetrics.VERSION, calculateFileChecksum(NUMERICAL_METRICS_RELATIVE_PATH))
        assertEquals(
            NUMERICAL_METRICS_EXPECTED_VERSION_AND_HASH, actualNumericalMetricsVersionAndHash,
            "Hash of ${NumericalMetrics::class.qualifiedName} has been changed, please increase VERSION value. " +
                    "Also you need to update hash and version in this test class."
        )
    }

    @Test
    fun testChecksTotalFilesChecksum() {
        val pathToExclude =
            setOf(
                Paths.get(STRING_METRICS_RELATIVE_PATH).toAbsolutePath().toString(),
                Paths.get(BOOLEAN_METRICS_RELATIVE_PATH).toAbsolutePath().toString(),
                Paths.get(NUMERICAL_METRICS_RELATIVE_PATH).toAbsolutePath().toString()
            )
        val actualSourceFolderVersionAndHash =
            Pair(
                NumericalMetrics.VERSION + StringMetrics.VERSION + BooleanMetrics.VERSION,
                calculateDirectoryCheckSum(SOURCE_CODE_RELATIVE_PATH, pathToExclude)
            )
        assertEquals(
            SOURCE_FOLDER_EXPECTED_VERSION_AND_HASH, actualSourceFolderVersionAndHash,
            "Hash of $SOURCE_CODE_RELATIVE_PATH has been changed, please increase VERSION value in one of the enums StringMetrics, NumericalMetrics, BooleanMetrics." +
                    "Also you need to update hash and version in this test class."
        )
    }

    private fun calculateFileChecksum(filePathStr: String): String {
        return calculateMD5HashForFileContent(Paths.get(filePathStr)).convertToHexString()
    }

    private fun calculateDirectoryCheckSum(
        dirPathStr: String,
        pathsToExclude: Set<String> = setOf(),
    ): String {
        return File(dirPathStr)
            .walk()
            .filter { file -> file.isFile }
            .filter { file -> !pathsToExclude.contains(file.absolutePath) }
            .sorted()
            .map { file -> calculateMD5HashForFileContent(file.toPath()) }
            .fold(byteArrayOf()) { acc, fileHash -> acc + fileHash }
            .getMD5Hash()
            .convertToHexString()
    }

    private fun calculateMD5HashForFileContent(path: Path): ByteArray {
        return Files.readAllBytes(path).getMD5Hash()
    }

    private fun ByteArray.getMD5Hash(): ByteArray {
        return MessageDigest.getInstance(HASH_ALG).digest(this)
    }

    private fun ByteArray.convertToHexString(): String {
        return this.joinToString("") { "%02x".format(it) }
    }

}