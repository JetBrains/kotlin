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
import org.jetbrains.kotlin.konan.blackboxtest.support.runner.*
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.*
import org.jetbrains.kotlin.konan.blackboxtest.support.util.*
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertEquals
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Tag

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
        // FIXME: remove the assumption below and fix failed test. Now `clang -fmodules` cannot compile cstubs.c using Darwin module from sysroot
        Assumptions.assumeFalse(this is AbstractNativeInteropIndexerFModulesTest && testPath.endsWith("/fullStdargH/"))

        val testPathFull = getAbsoluteFile(testPath)
        val testDataDir = testPathFull.parentFile.parentFile
        val includeFolder = testDataDir.resolve("include")
        val defFile = testPathFull.resolve("pod1.def")
        val goldenFile = testPathFull.resolve("contents.gold.txt")
        val fmodulesArgs = if (fmodules) listOf("-compiler-option", "-fmodules") else listOf()
        val includeFrameworkArgs = if (testDataDir.name == "framework")
            listOf("-compiler-option", "-F${testDataDir.canonicalPath}")
        else
            listOf("-compiler-option", "-I${includeFolder.canonicalPath}")

        val testCase: TestCase = generateCInteropTestCaseWithSingleDef(defFile, includeFrameworkArgs + fmodulesArgs)
        val testCompilationResult = testCase.cinteropToLibrary()
        val klibContents = testCompilationResult.resultingArtifact.getContents()

        val expectedContents = goldenFile.readText()
        assertEquals(StringUtilRt.convertLineSeparators(expectedContents), StringUtilRt.convertLineSeparators(klibContents)) {
            "Test failed. CInterop compilation result was: $testCompilationResult"
        }
    }
}
