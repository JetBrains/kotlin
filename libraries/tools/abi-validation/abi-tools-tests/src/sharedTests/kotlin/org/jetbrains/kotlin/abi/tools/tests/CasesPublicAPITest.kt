/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abi.tools.tests

import org.jetbrains.kotlin.abi.tools.AbiFilters
import org.junit.jupiter.api.*
import java.io.File
import kotlin.io.walk
import kotlin.test.Test
import kotlin.test.fail

class CasesPublicAPITest {

    @Test
    fun annotations(testInfo: TestInfo) {
        snapshotAPIAndCompare(testInfo)
    }

    @Test
    fun syntheticConstructors(testInfo: TestInfo) {
        snapshotAPIAndCompare(testInfo)
    }

    @Test
    fun companions(testInfo: TestInfo) {
        snapshotAPIAndCompare(testInfo, excludedAnnotatedWith = setOf("cases.companions.PrivateApi"))
    }

    @Test
    fun default(testInfo: TestInfo) {
        snapshotAPIAndCompare(testInfo, excludedAnnotatedWith = setOf("cases.default.PrivateApi"))
    }

    @Test
    fun inline(testInfo: TestInfo) {
        snapshotAPIAndCompare(testInfo)
    }

    @Test
    fun interfaces(testInfo: TestInfo) {
        snapshotAPIAndCompare(testInfo)
    }

    @Test
    fun internal(testInfo: TestInfo) {
        snapshotAPIAndCompare(testInfo)
    }

    @Test
    fun java(testInfo: TestInfo) {
        snapshotAPIAndCompare(testInfo)
    }

    @Test
    fun localClasses(testInfo: TestInfo) {
        snapshotAPIAndCompare(testInfo)
    }

    @Test
    fun marker(testInfo: TestInfo) {
        snapshotAPIAndCompare(
            testInfo,
            excludedAnnotatedWith = setOf(
                "cases.marker.HiddenField",
                "cases.marker.HiddenProperty",
                "cases.marker.HiddenMethod"
            )
        )
    }

    @Test
    fun nestedClasses(testInfo: TestInfo) {
        snapshotAPIAndCompare(testInfo)
    }

    @Test
    fun packageAnnotations(testInfo: TestInfo) {
        snapshotAPIAndCompare(testInfo, excludedAnnotatedWith = setOf("cases.packageAnnotations.PrivateApi"))
    }

    @Test
    fun private(testInfo: TestInfo) {
        snapshotAPIAndCompare(testInfo)
    }

    @Test
    fun protected(testInfo: TestInfo) {
        snapshotAPIAndCompare(testInfo)
    }

    @Test
    fun public(testInfo: TestInfo) {
        snapshotAPIAndCompare(testInfo)
    }

    @Test
    fun special(testInfo: TestInfo) {
        snapshotAPIAndCompare(testInfo)
    }

    @Test
    fun suspend(testInfo: TestInfo) {
        snapshotAPIAndCompare(testInfo)
    }

    @Test
    fun whenMappings(testInfo: TestInfo) {
        snapshotAPIAndCompare(testInfo)
    }

    @Test
    fun enums(testInfo: TestInfo) {
        snapshotAPIAndCompare(testInfo)
    }

    @Test
    fun repeatable(testInfo: TestInfo) {
        snapshotAPIAndCompare(testInfo, excludedClasses = setOf("cases.repeatable.RepeatableAnnotation.Container"))
    }

    @Test
    fun included(testInfo: TestInfo) {
        snapshotAPIAndCompare(testInfo, includedClasses = setOf("cases.included.subpackage.*"))
    }

    @Test
    fun root(testInfo: TestInfo) {
        snapshotAPIAndCompareRoot(testInfo, excludedClasses = setOf("RootClass1", "*Tests"), includedClasses = setOf("*"))
    }

    @Test
    fun file(testInfo: TestInfo) {
        snapshotAPIAndCompare(testInfo, excludedClasses = setOf("cases.file.FileFacade1Kt"))
    }

    @Test
    fun jvmOverloads(testInfo: TestInfo) {
        snapshotAPIAndCompare(testInfo)
    }

    @Test
    fun consts(testInfo: TestInfo) {
        snapshotAPIAndCompare(testInfo)
    }

    private fun snapshotAPIAndCompareRoot(
        testInfo: TestInfo,
        includedClasses: Set<String> = emptySet(),
        excludedClasses: Set<String> = emptySet(),
        includedAnnotatedWith: Set<String> = emptySet(),
        excludedAnnotatedWith: Set<String> = emptySet(),
    ) {
        val methodName = testInfo.testMethod.get().name
        val filters = AbiFilters(includedClasses, excludedClasses, includedAnnotatedWith, excludedAnnotatedWith)

        val testClassPaths = baseClassPaths.map { it.resolve("..") }
        val target = baseOutputPath.resolve(methodName).resolve("$methodName.txt")

        doCheck(testClassPaths, target, filters)
    }

    private fun snapshotAPIAndCompare(
        testInfo: TestInfo,
        includedClasses: Set<String> = emptySet(),
        excludedClasses: Set<String> = emptySet(),
        includedAnnotatedWith: Set<String> = emptySet(),
        excludedAnnotatedWith: Set<String> = emptySet(),
    ) {
        val methodName = testInfo.testMethod.get().name
        val filters = AbiFilters(includedClasses, excludedClasses, includedAnnotatedWith, excludedAnnotatedWith)

        val testClassPaths = baseClassPaths.map { it.resolve(methodName) }
        val target = baseOutputPath.resolve(methodName).resolve("$methodName.txt")

        doCheck(testClassPaths, target, filters)
    }

    companion object {
        val baseClassPaths: List<File> =
            System.getProperty("testCasesClassesDirs")
                .let { requireNotNull(it) { "Specify testCasesClassesDirs with a system property" } }
                .split(File.pathSeparator)
                .map { File(it, "cases").canonicalFile }
        val baseOutputPath = File("src/compiling/kotlin/cases")
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
