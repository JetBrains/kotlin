/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.support.group

import org.jetbrains.kotlin.konan.blackboxtest.support.*
import org.jetbrains.kotlin.konan.blackboxtest.support.TestCase.NoTestRunnerExtras
import org.jetbrains.kotlin.konan.blackboxtest.support.TestCase.WithTestRunnerExtras
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.GeneratedSources
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.Settings
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.TestRoots
import org.jetbrains.kotlin.konan.blackboxtest.support.util.DEFAULT_FILE_NAME
import org.jetbrains.kotlin.konan.blackboxtest.support.util.ThreadSafeFactory
import org.jetbrains.kotlin.konan.blackboxtest.support.util.computeGeneratedSourcesDir
import org.jetbrains.kotlin.konan.blackboxtest.support.util.computePackageName
import org.jetbrains.kotlin.test.directives.model.Directive
import org.jetbrains.kotlin.test.services.JUnit5Assertions
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertEquals
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertFalse
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertNotEquals
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertTrue
import org.jetbrains.kotlin.test.services.JUnit5Assertions.fail
import org.jetbrains.kotlin.test.services.impl.RegisteredDirectivesParser
import java.io.File

internal class StandardTestCaseGroupProvider(private val settings: Settings) : TestCaseGroupProvider {
    val sourceTransformers: MutableMap<String, List<(String) -> String>> = mutableMapOf()

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

    override fun setPreprocessors(testDataDir: File, preprocessors: List<(String) -> String>) {
        if (preprocessors.isNotEmpty())
            sourceTransformers[testDataDir.canonicalPath] = preprocessors
        else
            sourceTransformers.remove(testDataDir.canonicalPath)
    }

    override fun getTestCaseGroup(testDataDir: File) = lazyTestCaseGroups[testDataDir]

    private fun createTestCase(testDataFile: File): TestCase {
        val generatedSourcesDir = computeGeneratedSourcesDir(
            testDataBaseDir = settings.get<TestRoots>().baseDir,
            testDataFile = testDataFile,
            generatedSourcesBaseDir = settings.get<GeneratedSources>().testSourcesDir
        )

        val nominalPackageName = computePackageName(
            testDataBaseDir = settings.get<TestRoots>().baseDir,
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
                """
                    $location: Two declarations of the same module with different dependencies or friends found:
                    $testModule
                    $newTestModule
                """.trimIndent()
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
                    location = generatedSourcesDir.resolve(testModule.name).resolve(fileName),
                    module = testModule,
                    text = currentTestFileText
                )

                currentTestFileText.clear()
                repeat(location.lineNumber ?: 0) { currentTestFileText.appendLine() }
                currentTestFileName = null
            }
        }

        val text = testDataFile.applySourceTransformers(sourceTransformers[testDataFile.canonicalPath] ?: listOf())
        text.lines().forEachIndexed { lineNumber, line ->
            val location = Location(testDataFile, lineNumber)
            val expectFileDirectiveAfterModuleDirective =
                lastParsedDirective == TestDirectives.MODULE // Only FILE directive may follow MODULE directive.

            val rawDirective = RegisteredDirectivesParser.parseDirective(line)
            if (rawDirective != null) {
                val parsedDirective = try {
                    directivesParser.convertToRegisteredDirective(rawDirective)
                } catch (e: AssertionError) {
                    // Enhance error message with concrete test data file and line number where the error has happened.
                    throw AssertionError(
                        """
                            $location: Error while parsing directive in test data file.
                            Cause: ${e.message}
                        """.trimIndent(),
                        e
                    )
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
                // Was expecting a line with the FILE directive as this is the only possible continuation of a line with
                // the MODULE directive, but failed.
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

        val extras = when (testKind) {
            TestKind.REGULAR, TestKind.STANDALONE -> {
                // Fix package declarations to avoid unintended conflicts between symbols with the same name in different test cases.
                val needToFixPackageNames = testKind == TestKind.REGULAR

                val testFunctions = checkExistingPackageNamesAndCollectTestFunctions(
                    testModules = testModules.values,
                    basePackageName = nominalPackageName,
                    fixPackageNames = needToFixPackageNames,
                    testDataFile = testDataFile
                )

                WithTestRunnerExtras(testFunctions)
            }
            TestKind.STANDALONE_NO_TR -> NoTestRunnerExtras(
                entryPoint = parseEntryPoint(registeredDirectives, location),
                inputDataFile = parseInputDataFile(baseDir = testDataFile.parentFile, registeredDirectives, location)
            )
        }

        val testCase = TestCase(
            kind = testKind,
            modules = testModules.values.toSet(),
            freeCompilerArgs = freeCompilerArgs,
            origin = TestOrigin.SingleTestDataFile(testDataFile),
            nominalPackageName = nominalPackageName,
            expectedOutputDataFile = expectedOutputDataFile,
            extras = extras
        )
        testCase.initialize(findSharedModule = null)

        return testCase
    }

    companion object {
        private fun CharSequence.hasAnythingButComments(): Boolean {
            return dropNonMeaningfulLines().firstOrNull() != null
        }

        private fun checkExistingPackageNamesAndCollectTestFunctions(
            testModules: Collection<TestModule.Exclusive>,
            basePackageName: PackageFQN,
            fixPackageNames: Boolean,
            testDataFile: File
        ): List<TestFunction> {
            val testFunctions = mutableListOf<TestFunction>()

            testModules.forEach { testModule ->
                testModule.files.forEach { testFile ->
                    val meaningfulLines = testFile.text.dropNonMeaningfulLines()

                    // Retrieve the package name if it is declared inside the test file.
                    val existingPackageName = meaningfulLines.firstOrNull()?.let { (lineNumber, firstMeaningfulLine) ->
                        getExistingPackageName(lineNumber, firstMeaningfulLine, basePackageName, testDataFile)
                    }

                    // Compute the package name to be added (if it should be added, of course).
                    val packageNameToAdd = if (fixPackageNames && existingPackageName == null) basePackageName else null

                    // Collect test functions.
                    val effectivePackageName = existingPackageName ?: packageNameToAdd ?: ""
                    collectTestFunctionsNames(meaningfulLines, testDataFile) { functionName ->
                        testFunctions += TestFunction(effectivePackageName, functionName)
                    }

                    // Add package declaration (if necessary).
                    if (packageNameToAdd != null) {
                        testFile.update { text -> "package $packageNameToAdd $text" }
                    }
                }
            }

            return testFunctions
        }

        private fun getExistingPackageName(lineNumber: Int, line: String, basePackageName: PackageFQN, testDataFile: File): PackageFQN? {
            val existingPackageName = line
                .substringAfter("package ", missingDelimiterValue = "")
                .trimStart()
                .takeIf(String::isNotEmpty)
                ?: return null

            assertTrue(
                existingPackageName == basePackageName
                        || (existingPackageName.length > basePackageName.length
                        && existingPackageName.startsWith(basePackageName)
                        && existingPackageName[basePackageName.length] == '.')
            ) {
                """
                   ${Location(testDataFile, lineNumber)}: Invalid package name declaration found: $line
                    Expected: package $basePackageName
                """.trimIndent()
            }

            return existingPackageName
        }

        private inline fun collectTestFunctionsNames(
            meaningfulLines: Sequence<Pair<Int, String>>,
            testDataFile: File,
            crossinline consumeFunctionName: (FunctionName) -> Unit
        ) {
            var expectingTestFunction = false

            val extractFunctionName: (lineNumber: Int, line: String) -> Unit = { lineNumber, line ->
                val match = FUNCTION_REGEX.matchEntire(line)
                    ?: fail { "${Location(testDataFile, lineNumber)}: Can't parse name of test function: $line" }

                val functionName = match.groupValues[1]

                consumeFunctionName(functionName)
            }

            val processPossiblyAnnotatedLine: (lineNumber: Int, line: String) -> Unit = { lineNumber, line ->
                if (line.startsWith('@'))
                    for (testAnnotation in TEST_ANNOTATIONS) {
                        if (line.startsWith(testAnnotation)) {
                            val remainder = line.substringAfter(testAnnotation).trimStart()
                            if (remainder.isNotEmpty()) extractFunctionName(lineNumber, remainder) else expectingTestFunction = true
                            break
                        }
                    }
            }

            meaningfulLines.forEach { (lineNumber, line) ->
                if (expectingTestFunction) {
                    extractFunctionName(lineNumber, line)
                    expectingTestFunction = false
                } else
                    processPossiblyAnnotatedLine(lineNumber, line)
            }
        }

        private fun CharSequence.dropNonMeaningfulLines(): Sequence<Pair<Int, String>> {
            var inMultilineComment = false

            return lineSequence()
                .mapIndexed { lineNumber, line -> lineNumber to line.trim() }
                .dropWhile { (_, trimmedLine) ->
                    when {
                        inMultilineComment -> inMultilineComment = !trimmedLine.endsWith("*/")
                        trimmedLine.startsWith("/*") -> inMultilineComment = true
                        trimmedLine.isMeaningfulLine() -> return@dropWhile false
                    }
                    return@dropWhile true
                }
        }

        private fun String.isMeaningfulLine() = isNotEmpty() && !startsWith("//") && !startsWith("@file:")

        private val TEST_ANNOTATIONS = listOf("@Test", "@kotlin.test.Test")
        private val FUNCTION_REGEX = Regex("^\\s*fun\\s+(\\p{javaJavaIdentifierPart}+).*$")
    }
}
