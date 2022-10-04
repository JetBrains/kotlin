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
abstract class AbstractNativeInteropIndexerKT39120Test : AbstractNativeInteropIndexerBaseTest() {

    @Synchronized
    protected fun runTest(@TestDataFile testPath: String) {
        val testPathFull = getAbsoluteFile(testPath)
        val testDataDir = testPathFull.parentFile.parentFile
        val def1File = testPathFull.resolve("pod1.def")

        val includeFrameworkArgs = listOf("-compiler-option", "-F${testDataDir.canonicalPath}")

        generateCInteropTestCaseWithSingleDef(def1File, includeFrameworkArgs)
    }

    private fun filterContentsOutput(contents: String, pattern: String) =
        contents.split("\n").filter {
            it.contains(Regex(pattern))
        }.joinToString(separator = "\n")
}
