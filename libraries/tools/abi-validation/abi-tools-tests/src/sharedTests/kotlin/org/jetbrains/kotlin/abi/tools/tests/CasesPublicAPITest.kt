/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abi.tools.tests

import org.jetbrains.kotlin.abi.tools.AbiFilters
import org.junit.*
import org.junit.rules.TestName
import java.io.File
import kotlin.io.walk
import kotlin.test.fail

class CasesPublicAPITest {

    companion object {
        val baseClassPaths: List<File> =
            System.getProperty("testCasesClassesDirs")
                .let { requireNotNull(it) { "Specify testCasesClassesDirs with a system property" } }
                .split(File.pathSeparator)
                .map { File(it, "cases").canonicalFile }
        val baseOutputPath = File("src/compiling/kotlin/cases")
    }

    @Rule
    @JvmField
    val testName = TestName()

    @Test
    fun annotations() {
        snapshotAPIAndCompare(testName.methodName)
    }

    @Test
    fun syntheticConstructors() {
        snapshotAPIAndCompare(testName.methodName)
    }

    @Test
    fun companions() {
        snapshotAPIAndCompare(testName.methodName, excludedAnnotatedWith = setOf("cases.companions.PrivateApi"))
    }

    @Test
    fun default() {
        snapshotAPIAndCompare(testName.methodName, excludedAnnotatedWith = setOf("cases.default.PrivateApi"))
    }

    @Test
    fun inline() {
        snapshotAPIAndCompare(testName.methodName)
    }

    @Test
    fun interfaces() {
        snapshotAPIAndCompare(testName.methodName)
    }

    @Test
    fun internal() {
        snapshotAPIAndCompare(testName.methodName)
    }

    @Test
    fun java() {
        snapshotAPIAndCompare(testName.methodName)
    }

    @Test
    fun localClasses() {
        snapshotAPIAndCompare(testName.methodName)
    }

    @Test
    fun marker() {
        snapshotAPIAndCompare(
            testName.methodName,
            excludedAnnotatedWith = setOf(
                "cases.marker.HiddenField",
                "cases.marker.HiddenProperty",
                "cases.marker.HiddenMethod"
            )
        )
    }

    @Test
    fun nestedClasses() {
        snapshotAPIAndCompare(testName.methodName)
    }

    @Test
    fun packageAnnotations() {
        snapshotAPIAndCompare(testName.methodName, excludedAnnotatedWith = setOf("cases.packageAnnotations.PrivateApi"))
    }

    @Test
    fun private() {
        snapshotAPIAndCompare(testName.methodName)
    }

    @Test
    fun protected() {
        snapshotAPIAndCompare(testName.methodName)
    }

    @Test
    fun public() {
        snapshotAPIAndCompare(testName.methodName)
    }

    @Test
    fun special() {
        snapshotAPIAndCompare(testName.methodName)
    }

    @Test
    fun suspend() {
        snapshotAPIAndCompare(testName.methodName)
    }

    @Test
    fun whenMappings() {
        snapshotAPIAndCompare(testName.methodName)
    }

    @Test
    fun enums() {
        snapshotAPIAndCompare(testName.methodName)
    }

    @Test
    fun repeatable() {
        snapshotAPIAndCompare(testName.methodName, excludedClasses = setOf("cases.repeatable.RepeatableAnnotation.Container"))
    }

    @Test
    fun included() {
        snapshotAPIAndCompare(testName.methodName, includedClasses = setOf("cases.included.subpackage.*"))
    }

    @Test
    fun root() {
        snapshotAPIAndCompareRoot(testName.methodName, excludedClasses = setOf("RootClass1", "*Tests"), includedClasses = setOf("*"))
    }

    @Test
    fun file() {
        snapshotAPIAndCompare(testName.methodName, excludedClasses = setOf("cases.file.FileFacade1Kt"))
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
        val target = baseOutputPath.resolve(testClassRelativePath).resolve(testName.methodName + ".txt")

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
        val target = baseOutputPath.resolve(testClassRelativePath).resolve(testName.methodName + ".txt")

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
        fail("Expected data file did not exist. Generating: $target")
    } else {
        val stringBuffer = StringBuffer()
        AbiToolsImpl.printJvmDump(stringBuffer, inputFiles, filters)
        assertEqualsToFile(target, stringBuffer.toString())
    }
}
