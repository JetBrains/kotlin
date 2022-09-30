/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest

import com.intellij.openapi.util.text.StringUtilRt
import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.konan.blackboxtest.support.*
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.*
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationArtifact.*
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.blackboxtest.support.runner.*
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.*
import org.jetbrains.kotlin.konan.blackboxtest.support.util.*
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertEquals
import org.junit.jupiter.api.Tag
import java.io.File

abstract class AbstractNativeInteropIndexerFModulesTest : AbstractNativeInteropIndexerTest() {
    override val fmodules = true
}

abstract class AbstractNativeInteropIndexerNoFModulesTest : AbstractNativeInteropIndexerTest() {
    override val fmodules = false
}

@Tag("interop-indexer")
abstract class AbstractNativeInteropIndexerTest : AbstractNativeInteropIndexerBaseTest() {
    abstract val fmodules: Boolean

    @Synchronized
    protected fun runTest(@TestDataFile testPath: String) {
        val testPathFull = getAbsoluteFile(testPath)
        val testDataDir = testPathFull.parentFile.parentFile
        val includeFolder = testDataDir.resolve("include")
        val defFile = testPathFull.resolve("pod1.def")
        val fmodulesArgs = if (fmodules) listOf("-compiler-option", "-fmodules") else listOf()
        val includeFrameworkArgs = if (testDataDir.name == "simple")
            listOf("-compiler-option", "-I${includeFolder.canonicalPath}")
        else
            listOf("-compiler-option", "-F${testDataDir.canonicalPath}")

        val testCase: TestCase = generateCInteropTestCaseWithSingleDef(defFile, includeFrameworkArgs + fmodulesArgs)
        val klib: KLIB = testCase.cinteropToLibrary().resultingArtifact

        val klibContents = invokeKLibContents(klib.klibFile)

        val expectedOutput = testPathFull.resolve("contents.gold.txt").readText()
        assertEquals(StringUtilRt.convertLineSeparators(expectedOutput), StringUtilRt.convertLineSeparators(klibContents))
    }
}
