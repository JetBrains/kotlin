/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest

import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.konan.blackboxtest.support.*
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.*
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationArtifact.*
import org.jetbrains.kotlin.konan.blackboxtest.support.runner.InteropTestRunner
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.*
import org.jetbrains.kotlin.konan.blackboxtest.support.util.*
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.junit.jupiter.api.Tag
import java.io.File
import kotlin.io.path.createTempDirectory

@Tag("cinterop")
abstract class AbstractNativeInteropIndexerTest : AbstractNativeSimpleTest() {

    protected fun runTest(@TestDataFile testPath: String): Unit {
        val fmodules: Boolean = false
        val homeDirectory = KtTestUtil.getHomeDirectory()
        println("homeDirectory: $homeDirectory testPath: $testPath")
        val testCanonicalFile = File(homeDirectory).resolve(testPath)
        val testParentFile = testCanonicalFile.parentFile.parentFile
        val testDataPath = testParentFile.canonicalPath
        println("testDataPath: $testDataPath")
        val canonicalPath = testParentFile.resolve("cinterop_contents.sh").canonicalPath
        val programArgs = listOf(
            canonicalPath,
            "-o", "klib.klib",
            "-def", testCanonicalFile.resolve("pod1.def").canonicalPath,
            "-compiler-option", "-F$testDataPath", "-compiler-option", "-I$testDataPath/include"
        )
        val fmodulesArgs = if (fmodules) listOf("-compiler-option", "-fmodules") else listOf()
        val testDir = createTempDirectory("${AbstractNativeInteropIndexerTest::class.java.sanitizedName}-${testCanonicalFile.name}").toFile()
        InteropTestRunner(programArgs + fmodulesArgs, testDir).run()
    }
}
