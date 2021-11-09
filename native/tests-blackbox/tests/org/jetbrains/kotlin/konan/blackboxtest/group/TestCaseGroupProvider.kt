/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.group

import org.jetbrains.kotlin.konan.blackboxtest.*
import org.jetbrains.kotlin.konan.blackboxtest.util.DEFAULT_FILE_NAME
import org.jetbrains.kotlin.konan.blackboxtest.util.ThreadSafeFactory
import org.jetbrains.kotlin.konan.blackboxtest.util.computeGeneratedSourcesDir
import org.jetbrains.kotlin.konan.blackboxtest.util.computePackageName
import org.jetbrains.kotlin.test.directives.model.Directive
import org.jetbrains.kotlin.test.services.JUnit5Assertions
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertEquals
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertFalse
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertNotEquals
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertTrue
import org.jetbrains.kotlin.test.services.JUnit5Assertions.fail
import org.jetbrains.kotlin.test.services.impl.RegisteredDirectivesParser
import java.io.File

internal interface TestCaseGroupProvider {
    fun getTestCaseGroup(testDataDir: File): TestCaseGroup?
}

internal class StandardTestCaseGroupProvider(private val environment: TestEnvironment) : TestCaseGroupProvider {
    // Load test cases in groups on demand.
    private val lazyTestCaseGroups = ThreadSafeFactory<File, TestCaseGroup?> { testDataDir ->
        val testDataFiles = testDataDir.listFiles()
            ?: return@ThreadSafeFactory null // `null` means that there is no such testDataDir.

        val testCases = testDataFiles.mapNotNull { testDataFile ->
            if (!testDataFile.isFile || testDataFile.extension != "kt")
                return@mapNotNull null

            createTestCase(testDataFile)
        }

        TestCaseGroup.Default(disabledTestDataFileNames = emptySet(), testCases = testCases)
    }

    override fun getTestCaseGroup(testDataDir: File) = lazyTestCaseGroups[testDataDir]

    private fun createTestCase(testDataFile: File): TestCase {
        val generatedSourcesDir = computeGeneratedSourcesDir(
            testDataBaseDir = environment.testRoots.baseDir,
            testDataFile = testDataFile,
            generatedSourcesBaseDir = environment.testSourcesDir
        )

        val effectivePackageName = computePackageName(
            testDataBaseDir = environment.testRoots.baseDir,
            testDataFile = testDataFile
        )

        val testModules = hashMapOf<String, TestModule.Exclusive>()
        var currentTestModule: TestModule.Exclusive? = null

        var currentTestFileName: String? = null
        val currentTestFileText = StringBuilder()

        val directivesParser = RegisteredDirectivesParser(TestDirectives, JUnit5Assertions)
        var lastParsedDirective: Directive? = null

        fun switchTestModule(newTestModule: TestModule.Exclusive, location: Location): TestModule.Exclusive {
            // Don't register new test module if there is another one with the same name.
            val testModule = testModules.getOrPut(newTestModule.name) { newTestModule }
            assertTrue(testModule === newTestModule || testModule.haveSameSymbols(newTestModule)) {
                "$location: Two declarations of the same module with different dependencies or friends found:\n$testModule\n$newTestModule"
            }

            currentTestModule = testModule
            return testModule
        }

        fun beginTestFile(fileName: String) {
            assertEquals(null, currentTestFileName)
            currentTestFileName = fileName
        }

        fun finishTestFile(forceFinish: Boolean, location: Location) {
            val needToFinish = forceFinish
                    || currentTestFileName != null
                    || (currentTestFileName == null /*&& testFiles.isEmpty()*/ && currentTestFileText.hasAnythingButComments())

            if (needToFinish) {
                val fileName = currentTestFileName ?: DEFAULT_FILE_NAME
                val testModule = currentTestModule ?: switchTestModule(TestModule.newDefaultModule(), location)

                testModule.files += TestFile.createUncommitted(
                    location = generatedSourcesDir.resolve(fileName),
                    module = testModule,
                    text = currentTestFileText
                )

                currentTestFileText.clear()
                repeat(location.lineNumber ?: 0) { currentTestFileText.appendLine() }
                currentTestFileName = null
            }
        }

        testDataFile.readLines().forEachIndexed { lineNumber, line ->
            val location = Location(testDataFile, lineNumber)
            val expectFileDirectiveAfterModuleDirective =
                lastParsedDirective == TestDirectives.MODULE // Only FILE directive may follow MODULE directive.

            val rawDirective = RegisteredDirectivesParser.parseDirective(line)
            if (rawDirective != null) {
                val parsedDirective = try {
                    directivesParser.convertToRegisteredDirective(rawDirective)
                } catch (e: AssertionError) {
                    // Enhance error message with concrete test data file and line number where the error has happened.
                    throw AssertionError("$location: Error while parsing directive in test data file.\nCause: ${e.message}", e)
                }

                if (parsedDirective != null) {
                    when (val directive = parsedDirective.directive) {
                        TestDirectives.FILE -> {
                            val newFileName = parseFileName(parsedDirective, location)
                            finishTestFile(forceFinish = false, location)
                            beginTestFile(newFileName)
                        }
                        else -> {
                            assertFalse(expectFileDirectiveAfterModuleDirective) {
                                "$location: Directive $directive encountered after ${TestDirectives.MODULE} directive but was expecting ${TestDirectives.FILE}"
                            }

                            when (directive) {
                                TestDirectives.MODULE -> {
                                    finishTestFile(forceFinish = false, location)
                                    switchTestModule(parseModule(parsedDirective, location), location)
                                }
                                else -> {
                                    assertNotEquals(TestDirectives.FILE, lastParsedDirective) {
                                        "$location: Global directive $directive encountered after ${TestDirectives.FILE} directive"
                                    }
                                    assertNotEquals(TestDirectives.MODULE, lastParsedDirective) {
                                        "$location: Global directive $directive encountered after ${TestDirectives.MODULE} directive"
                                    }

                                    directivesParser.addParsedDirective(parsedDirective)
                                }
                            }
                        }
                    }

                    currentTestFileText.appendLine()
                    lastParsedDirective = parsedDirective.directive
                    return@forEachIndexed
                }
            }

            if (expectFileDirectiveAfterModuleDirective) {
                // Was expecting a line with the FILE directive as this is the only possible continuation of a line with the MODULE directive, but failed.
                fail { "$location: ${TestDirectives.FILE} directive expected after ${TestDirectives.MODULE} directive" }
            }

            currentTestFileText.appendLine(line)
        }

        val location = Location(testDataFile)
        finishTestFile(forceFinish = true, location)

        val registeredDirectives = directivesParser.build()

        val freeCompilerArgs = parseFreeCompilerArgs(registeredDirectives, location)
        val expectedOutputDataFile = parseOutputDataFile(baseDir = testDataFile.parentFile, registeredDirectives, location)
        val testKind = parseTestKind(registeredDirectives, location)

        if (testKind == TestKind.REGULAR) {
            // Fix package declarations to avoid unintended conflicts between symbols with the same name in different test cases.
            testModules.values.forEach { testModule ->
                testModule.files.forEach { testFile -> fixPackageDeclaration(testFile, effectivePackageName, testDataFile) }
            }
        }

        val testCase = TestCase(
            kind = testKind,
            modules = testModules.values.toSet(),
            freeCompilerArgs = freeCompilerArgs,
            origin = TestOrigin.SingleTestDataFile(testDataFile),
            nominalPackageName = effectivePackageName,
            expectedOutputDataFile = expectedOutputDataFile,
            extras = if (testKind == TestKind.STANDALONE_NO_TR) {
                TestCase.StandaloneNoTestRunnerExtras(
                    entryPoint = parseEntryPoint(registeredDirectives, location),
                    inputDataFile = parseInputDataFile(baseDir = testDataFile.parentFile, registeredDirectives, location)
                )
            } else
                null
        )
        testCase.initialize(findSharedModule = null)

        return testCase
    }

    private fun CharSequence.hasAnythingButComments(): Boolean {
        var result = false
        runForFirstMeaningfulStatement { _, _ -> result = true }
        return result
    }

    private fun fixPackageDeclaration(
        testFile: TestFile<TestModule.Exclusive>,
        packageName: PackageName,
        testDataFile: File
    ) = testFile.update { text ->
        var existingPackageDeclarationLine: String? = null
        var existingPackageDeclarationLineNumber: Int? = null

        text.runForFirstMeaningfulStatement { lineNumber, line ->
            // First meaningful line.
            val trimmedLine = line.trim()
            if (trimmedLine.startsWith("package ")) {
                existingPackageDeclarationLine = trimmedLine
                existingPackageDeclarationLineNumber = lineNumber
            }
        }

        if (existingPackageDeclarationLine != null) {
            val existingPackageName = existingPackageDeclarationLine!!.substringAfter("package ").trimStart()
            assertTrue(
                existingPackageName == packageName
                        || (existingPackageName.length > packageName.length
                        && existingPackageName.startsWith(packageName)
                        && existingPackageName[packageName.length] == '.')
            ) {
                val location = Location(testDataFile, existingPackageDeclarationLineNumber)
                "$location: Invalid package name declaration found: $existingPackageDeclarationLine\nExpected: package $packageName"

            }
            text
        } else
            "package $packageName $text"
    }

    private inline fun CharSequence.runForFirstMeaningfulStatement(action: (lineNumber: Int, line: String) -> Unit) {
        var inMultilineComment = false

        for ((lineNumber, line) in lines().withIndex()) {
            val trimmedLine = line.trim()
            when {
                inMultilineComment -> inMultilineComment = !trimmedLine.endsWith("*/")
                trimmedLine.startsWith("/*") -> inMultilineComment = true
                trimmedLine.isEmpty() -> Unit
                trimmedLine.startsWith("//") -> Unit
                trimmedLine.startsWith("@file:") -> Unit
                else -> {
                    action(lineNumber, line)
                    break
                }
            }
        }
    }
}
