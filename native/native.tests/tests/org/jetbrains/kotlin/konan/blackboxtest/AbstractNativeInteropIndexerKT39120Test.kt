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
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.*
import org.jetbrains.kotlin.konan.blackboxtest.support.util.*
import org.junit.jupiter.api.Tag
import org.jetbrains.kotlin.konan.blackboxtest.support.runner.*
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertEquals
import org.junit.jupiter.api.Assumptions

@Tag("interop-indexer")
abstract class AbstractNativeInteropIndexerKT39120Test : AbstractNativeInteropIndexerBaseTest() {

    @Synchronized
    protected fun runTest(@TestDataFile testPath: String) {
        Assumptions.assumeTrue(!HostManager.host.family.isAppleFamily) // KT-39120 is about Objective-C, so under Apple hosts only

        val testPathFull = getAbsoluteFile(testPath)
        val testDataDir = testPathFull.parentFile.parentFile
        val def1File = testPathFull.resolve("pod1.def")
        val def2File = testPathFull.resolve("pod2.def")
        val golden1File = testPathFull.resolve("pod1.contents.gold.txt")
        val golden2File = testPathFull.resolve("pod2.contents.gold.txt")

        val includeFrameworkArgs = listOf("-compiler-option", "-F${testDataDir.canonicalPath}")

        val test1Case: TestCase = generateCInteropTestCaseWithSingleDef(def1File, includeFrameworkArgs)
        val klib1: KLIB = test1Case.cinteropToLibrary().resultingArtifact
        val contents1 = klib1.getContents()

        val expectedFiltered1Output = golden1File.readText()
        val actualFiltered1Output = filterContentsOutput(contents1, " pod.Version")
        assertEquals(StringUtilRt.convertLineSeparators(expectedFiltered1Output), StringUtilRt.convertLineSeparators(actualFiltered1Output))

        val cinterop2ExtraArgs = listOf("-l", klib1.klibFile.canonicalPath, "-compiler-option", "-fmodules")
        val test2Case: TestCase = generateCInteropTestCaseWithSingleDef(def2File, includeFrameworkArgs + cinterop2ExtraArgs)
        val klib2: KLIB = test2Case.cinteropToLibrary().resultingArtifact
        val contents2 = klib2.getContents()

        val expectedFiltered2Output = golden2File.readText()
        val actualFiltered2Output = filterContentsOutput(contents2, " pod.Version")
        assertEquals(StringUtilRt.convertLineSeparators(expectedFiltered2Output), StringUtilRt.convertLineSeparators(actualFiltered2Output))
    }

    private fun filterContentsOutput(contents: String, pattern: String) =
        contents.split("\n").filter {
            it.contains(Regex(pattern))
        }.joinToString(separator = "\n")
}
