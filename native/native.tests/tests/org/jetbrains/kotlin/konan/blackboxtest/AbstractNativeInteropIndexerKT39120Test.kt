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
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertEquals

@Tag("interop-indexer")
abstract class AbstractNativeInteropIndexerKT39120Test : AbstractNativeSimpleTest() {

    @Synchronized
    protected fun runTest(@TestDataFile testPath: String) {
        val testPathFull = getAbsoluteFile(testPath)
        val testDataDir = testPathFull.parentFile.parentFile
        val def1File = testPathFull.resolve("pod1.def")
        val def2File = testPathFull.resolve("pod2.def")

        val testBuildDir = testRunSettings.get<SimpleTestDirectories>().testBuildDir
        val klib1File = testBuildDir.resolve("pod1.klib")
        val klib2File = testBuildDir.resolve("pod2.klib")

        val includeFrameworkArgs = arrayOf("-compiler-option", "-F${testDataDir.canonicalPath}")
        val cinterop2ExtraArgs = arrayOf("-l", klib1File.canonicalPath, "-compiler-option", "-fmodules")

        invokeCInterop(def1File, klib1File, includeFrameworkArgs)
        invokeCInterop(def2File, klib2File, includeFrameworkArgs + cinterop2ExtraArgs)

        val contents1 = invokeKLibContents(klib1File)
        val contents2 = invokeKLibContents(klib2File)

        val expectedEssentialOutput = testPathFull.resolve("contents.gold.txt").readText().trim()
        val essentialOutput = (contents1 + contents2).split("\n").filter {
            it.contains(Regex("package pod| pod.Version"))
        }.joinToString(separator = "\n")
        assertEquals(StringUtilRt.convertLineSeparators(expectedEssentialOutput), StringUtilRt.convertLineSeparators(essentialOutput))
    }
}
