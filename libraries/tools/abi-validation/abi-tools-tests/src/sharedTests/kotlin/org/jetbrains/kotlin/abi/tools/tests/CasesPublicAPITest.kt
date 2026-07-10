/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abi.tools.tests

import org.jetbrains.kotlin.abi.tools.AbiFilters
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo
import java.io.File
import kotlin.test.Test

class CasesPublicAPITest {

    companion object {
        val baseClassPaths: List<File> =
            System.getProperty("testCasesClassesDirs")
                .let { requireNotNull(it) { "Specify testCasesClassesDirs with a system property" } }
                .split(File.pathSeparator)
                .map { File(it, "cases").canonicalFile }
        val baseOutputPath = File("src/compiling/kotlin/cases")
    }

    private lateinit var testMethodName: String

    @BeforeEach
    fun captureTestName(testInfo: TestInfo) {
        testMethodName = testInfo.testMethod.get().name
    }

    @Test
    fun annotations() {
        snapshotAPIAndCompare(testMethodName)
    }

    @Test
    fun syntheticConstructors() {
        snapshotAPIAndCompare(testMethodName)
    }

    @Test
    fun companions() {
        snapshotAPIAndCompare(testMethodName, excludedAnnotatedWith = setOf("cases.companions.PrivateApi"))
    }

    @Test
    fun default() {
        snapshotAPIAndCompare(testMethodName, excludedAnnotatedWith = setOf("cases.default.PrivateApi"))
    }

    @Test
    fun inline() {
        snapshotAPIAndCompare(testMethodName)
    }

    @Test
    fun interfaces() {
        snapshotAPIAndCompare(testMethodName)
    }

    @Test
    fun internal() {
        snapshotAPIAndCompare(testMethodName)
    }

    @Test
    fun java() {
        snapshotAPIAndCompare(testMethodName)
    }

    @Test
    fun localClasses() {
        snapshotAPIAndCompare(testMethodName)
    }

    @Test
    fun marker() {
        snapshotAPIAndCompare(
            testMethodName,
            excludedAnnotatedWith = setOf(
                "cases.marker.HiddenField",
                "cases.marker.HiddenProperty",
                "cases.marker.HiddenMethod"
            )
        )
    }

    @Test
    fun nestedClasses() {
        snapshotAPIAndCompare(testMethodName)
    }

    @Test
    fun packageAnnotations() {
        snapshotAPIAndCompare(testMethodName, excludedAnnotatedWith = setOf("cases.packageAnnotations.PrivateApi"))
    }

    @Test
    fun private() {
        snapshotAPIAndCompare(testMethodName)
    }

    @Test
    fun protected() {
        snapshotAPIAndCompare(testMethodName)
    }

    @Test
    fun public() {
        snapshotAPIAndCompare(testMethodName)
    }

    @Test
    fun special() {
        snapshotAPIAndCompare(testMethodName)
    }

    @Test
    fun suspend() {
        snapshotAPIAndCompare(testMethodName)
    }

    @Test
    fun whenMappings() {
        snapshotAPIAndCompare(testMethodName)
    }

    @Test
    fun enums() {
        snapshotAPIAndCompare(testMethodName)
    }

    @Test
    fun repeatable() {
        snapshotAPIAndCompare(testMethodName, excludedClasses = setOf("cases.repeatable.RepeatableAnnotation.Container"))
    }

    @Test
    fun included() {
        snapshotAPIAndCompare(testMethodName, includedClasses = setOf("cases.included.subpackage.*"))
    }

    @Test
    fun root() {
        snapshotAPIAndCompareRoot(testMethodName, excludedClasses = setOf("RootClass1", "*Tests"), includedClasses = setOf("*"))
    }

    @Test
    fun file() {
        snapshotAPIAndCompare(testMethodName, excludedClasses = setOf("cases.file.FileFacade1Kt"))
    }

    @Test
    fun jvmOverloads() {
        snapshotAPIAndCompare(testMethodName)
    }

    @Test
    fun consts() {
        snapshotAPIAndCompare(testMethodName)
    }

    private fun snapshotAPIAndCompareRoot(
        testClassRelativePath: String,
        includedClasses: Set<String> = emptySet(),
        excludedClasses: Set<String> = emptySet(),
        includedAnnotatedWith: Set<String> = emptySet(),
        excludedAnnotatedWith: Set<String> = emptySet(),
    ) {
        val filters = AbiFilters(includedClasses, excludedClasses, includedAnnotatedWith, excludedAnnotatedWith)

        val testClassPaths = baseClassPaths.map { it.resolve("..") }
        val target = baseOutputPath.resolve(testClassRelativePath).resolve(testMethodName + ".txt")

        doCheck(testClassPaths, target, filters)
    }

    private fun snapshotAPIAndCompare(
        testClassRelativePath: String,
        includedClasses: Set<String> = emptySet(),
        excludedClasses: Set<String> = emptySet(),
        includedAnnotatedWith: Set<String> = emptySet(),
        excludedAnnotatedWith: Set<String> = emptySet(),
    ) {
        val filters = AbiFilters(includedClasses, excludedClasses, includedAnnotatedWith, excludedAnnotatedWith)

        val testClassPaths = baseClassPaths.map { it.resolve(testClassRelativePath) }
        val target = baseOutputPath.resolve(testClassRelativePath).resolve(testMethodName + ".txt")

        doCheck(testClassPaths, target, filters)
    }
}

internal fun doCheck(testClassPaths: List<File>, target: File, filters: AbiFilters) {
    val inputFiles = testClassPaths.flatMap { it.walk() }.filter { it.extension == "class" || it.extension == "jar" }
    check(inputFiles.isNotEmpty()) { "No class or jar files are found in paths: $testClassPaths" }

    if (!target.exists()) {
        target.bufferedWriter().use { writer ->
            AbiToolsImpl.printJvmDump(writer, inputFiles, filters)
        }
        error("Expected data file did not exist. Generating: $target")
    } else {
        val stringBuffer = StringBuffer()
        AbiToolsImpl.printJvmDump(stringBuffer, inputFiles, filters)
        assertEqualsToFile(target, stringBuffer.toString())
    }
}
