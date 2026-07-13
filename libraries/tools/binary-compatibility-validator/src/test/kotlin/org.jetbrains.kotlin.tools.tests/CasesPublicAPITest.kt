/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.tests

import kotlinx.validation.api.filterOutNonPublic
import kotlinx.validation.api.loadApiFromJvmClasses
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import java.io.File

class CasesPublicAPITest {

    companion object {
        val baseClassPaths: List<File> =
            System.getProperty("testCasesClassesDirs")
                .let { requireNotNull(it) { "Specify testCasesClassesDirs with a system property"} }
                .split(File.pathSeparator)
                .map { File(it, "cases").canonicalFile }
        val baseOutputPath = File("src/test/kotlin/cases")
    }

    private lateinit var testMethodName: String

    @BeforeEach
    fun captureTestName(testInfo: TestInfo) {
        testMethodName = testInfo.testMethod.get().name
    }

    @Test fun companions() { snapshotAPIAndCompare(testMethodName) }

    @Test fun default() { snapshotAPIAndCompare(testMethodName) }

    @Test fun inline() { snapshotAPIAndCompare(testMethodName) }

    @Test fun interfaces() { snapshotAPIAndCompare(testMethodName) }

    @Test fun internal() { snapshotAPIAndCompare(testMethodName) }

    @Test fun java() { snapshotAPIAndCompare(testMethodName) }

    @Test fun localClasses() { snapshotAPIAndCompare(testMethodName) }

    @Test fun nestedClasses() { snapshotAPIAndCompare(testMethodName) }

    @Test fun private() { snapshotAPIAndCompare(testMethodName) }

    @Test fun protected() { snapshotAPIAndCompare(testMethodName) }

    @Test fun public() { snapshotAPIAndCompare(testMethodName) }

    @Test fun special() { snapshotAPIAndCompare(testMethodName) }

    @Test fun whenMappings() { snapshotAPIAndCompare(testMethodName) }


    private fun snapshotAPIAndCompare(testClassRelativePath: String) {
        val testClassPaths = baseClassPaths.map { it.resolve(testClassRelativePath) }
        val testClasses = testClassPaths.flatMap { it.listFiles().orEmpty().asIterable() }
        check(testClasses.isNotEmpty()) { "No class files are found in paths: $testClassPaths" }

        val testClassStreams = testClasses.asSequence().filter { it.name.endsWith(".class") }.map { it.inputStream() }

        val api = testClassStreams.loadApiFromJvmClasses().filterOutNonPublic()

        val target = baseOutputPath.resolve(testClassRelativePath).resolve(testMethodName + ".txt")

        api.dumpAndCompareWith(target)
    }
}
