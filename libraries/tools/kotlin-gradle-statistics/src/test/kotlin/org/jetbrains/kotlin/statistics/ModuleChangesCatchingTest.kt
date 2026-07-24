/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.statistics

import org.jetbrains.kotlin.statistics.metrics.BooleanMetrics
import org.jetbrains.kotlin.statistics.metrics.NumericalMetrics
import org.jetbrains.kotlin.statistics.metrics.StringListMetrics
import org.jetbrains.kotlin.statistics.metrics.StringMetrics
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest
import kotlin.reflect.KClass

private const val SOURCE_CODE_RELATIVE_PATH =
    "libraries/tools/kotlin-gradle-statistics/src/main/kotlin/org/jetbrains/kotlin/statistics/metrics"
private const val BOOLEAN_METRICS_RELATIVE_PATH = "$SOURCE_CODE_RELATIVE_PATH/BooleanMetrics.kt"
private const val STRING_METRICS_RELATIVE_PATH = "$SOURCE_CODE_RELATIVE_PATH/StringMetrics.kt"
private const val NUMERICAL_METRICS_RELATIVE_PATH = "$SOURCE_CODE_RELATIVE_PATH/NumericalMetrics.kt"
private const val STRING_LIST_METRICS_RELATIVE_PATH = "$SOURCE_CODE_RELATIVE_PATH/StringListMetrics.kt"

private val STRING_METRICS_EXPECTED_VERSION_AND_HASH = Pair(12, "3ae4ad64e1d61f2627284d3f518e2d5e")
private val BOOLEAN_METRICS_EXPECTED_VERSION_AND_HASH = Pair(29, "1996c736f9263217f073e662baf41373")
private val NUMERICAL_METRICS_EXPECTED_VERSION_AND_HASH = Pair(3, "bcc6f0dba7d9db8e58408f8c0ff57965")
private val STRING_LIST_METRICS_EXPECTED_VERSION_AND_HASH = Pair(3, "335d70da85c85cd3c9f9fd38f3d96a4e")
private val SOURCE_FOLDER_EXPECTED_VERSION_AND_HASH =
    Pair(
        STRING_METRICS_EXPECTED_VERSION_AND_HASH.first +
                BOOLEAN_METRICS_EXPECTED_VERSION_AND_HASH.first +
                NUMERICAL_METRICS_EXPECTED_VERSION_AND_HASH.first + STRING_LIST_METRICS_EXPECTED_VERSION_AND_HASH.first,
        "626062cba4ec71e2306715a0a16909f3"
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
            errorMessage(StringMetrics::class)
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
            errorMessage(BooleanMetrics::class)
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
            errorMessage(NumericalMetrics::class)
        )
    }

    /**
     * Test checks for that the version of [StringListMetrics] was increased after changes in this file
     */
    @Test
    fun testChecksCorrectChangingStringListMetricsVersion() {
        val actualVersionAndHash =
            Pair(StringListMetrics.VERSION, calculateFileChecksum(STRING_LIST_METRICS_RELATIVE_PATH))
        assertEquals(
            STRING_LIST_METRICS_EXPECTED_VERSION_AND_HASH, actualVersionAndHash,
            errorMessage(StringListMetrics::class)
        )
    }

    private fun errorMessage(clazz: KClass<*>): String =
        "Hash of ${clazz.qualifiedName} has been changed, please increase VERSION value. " +
                "Also you need to update hash and version in this test class."

    @Test
    fun testChecksTotalFilesChecksum() {
        val pathToExclude =
            setOf(
                Paths.get(STRING_METRICS_RELATIVE_PATH).toAbsolutePath().toString(),
                Paths.get(BOOLEAN_METRICS_RELATIVE_PATH).toAbsolutePath().toString(),
                Paths.get(NUMERICAL_METRICS_RELATIVE_PATH).toAbsolutePath().toString(),
                Paths.get(STRING_LIST_METRICS_RELATIVE_PATH).toAbsolutePath().toString()
            )
        val actualSourceFolderVersionAndHash =
            Pair(
                NumericalMetrics.VERSION + StringMetrics.VERSION + BooleanMetrics.VERSION + StringListMetrics.VERSION,
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
