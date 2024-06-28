/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.util

import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertTrue
import org.jetbrains.kotlin.test.util.KtTestUtil.getHomeDirectory
import java.io.File

internal fun File.ensureExistsAndIsEmptyDirectory(): File {
    if (exists()) deleteRecursively()
    mkdirs()
    return this
}

internal fun getAbsoluteFile(localPath: String): File = File(getHomeDirectory()).resolve(localPath)

internal fun computeGeneratedSourcesDir(testDataBaseDir: File, testDataFile: File, generatedSourcesBaseDir: File): File {
    assertTrue(testDataFile.startsWith(testDataBaseDir)) {
        """
            The file is outside of the directory.
            File: $testDataFile
            Directory: $testDataBaseDir
        """.trimIndent()
    }

    val testDataFileDir = testDataFile.parentFile
    return generatedSourcesBaseDir
        .resolve(testDataFileDir.relativeTo(testDataBaseDir))
        .resolve(testDataFile.nameWithoutExtension)
}

internal fun generateBoxFunctionLauncher(entryPointFunctionFQN: String, expectedResult: String = "OK"): String =
    """
        @kotlin.contracts.ExperimentalContracts  // for tests in compiler/testData/codegen/box/contracts/
        @kotlinx.cinterop.ExperimentalForeignApi // for tests in native/native.tests/testData/codegen/cinterop/
        @kotlin.ExperimentalStdlibApi            // for test native/native.tests/testData/codegen/fileCheck/bce.kt
        @kotlin.test.Test
        fun runTest() {
            val result = $entryPointFunctionFQN()
            kotlin.test.assertEquals("$expectedResult", result, "Test failed with: ${'$'}result")
        }
        
    """.trimIndent()
