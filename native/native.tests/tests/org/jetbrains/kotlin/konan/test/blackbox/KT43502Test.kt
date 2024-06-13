/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("JUnitTestCaseWithNoTests")

package org.jetbrains.kotlin.konan.test.blackbox

import com.intellij.testFramework.TestDataPath
import org.jetbrains.kotlin.konan.test.blackbox.support.*
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationFactory
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.group.FirPipeline
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestExecutable
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunCheck
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunChecks
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.BinaryLibraryKind
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.Timeouts
import org.jetbrains.kotlin.konan.test.blackbox.support.util.assumeLibraryKindSupported
import org.jetbrains.kotlin.konan.test.blackbox.support.util.compileWithClang
import org.jetbrains.kotlin.konan.test.blackbox.support.util.compileWithClangToStaticLibrary
import org.jetbrains.kotlin.konan.test.blackbox.support.util.getKindSpecificClangFlags
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.File

@EnforcedProperty(property = ClassLevelProperty.BINARY_LIBRARY_KIND, propertyValue = "DYNAMIC")
@EnforcedProperty(property = ClassLevelProperty.C_INTERFACE_MODE, propertyValue = "V1")
@TestMetadata("native/native.tests/testData/kt43502")
@TestDataPath("\$PROJECT_ROOT")
class ClassicDynamicKT43502Test : KT43502TestBase()

@EnforcedProperty(property = ClassLevelProperty.BINARY_LIBRARY_KIND, propertyValue = "STATIC")
@EnforcedProperty(property = ClassLevelProperty.C_INTERFACE_MODE, propertyValue = "V1")
@TestMetadata("native/native.tests/testData/kt43502")
@TestDataPath("\$PROJECT_ROOT")
class ClassicStaticKT43502Test : KT43502TestBase()

@EnforcedProperty(property = ClassLevelProperty.BINARY_LIBRARY_KIND, propertyValue = "DYNAMIC")
@EnforcedProperty(property = ClassLevelProperty.C_INTERFACE_MODE, propertyValue = "V1")
@TestMetadata("native/native.tests/testData/kt43502")
@TestDataPath("\$PROJECT_ROOT")
@Tag("frontend-fir")
@FirPipeline()
class FirDynamicKT43502Test : KT43502TestBase()

@EnforcedProperty(property = ClassLevelProperty.BINARY_LIBRARY_KIND, propertyValue = "STATIC")
@EnforcedProperty(property = ClassLevelProperty.C_INTERFACE_MODE, propertyValue = "V1")
@TestMetadata("native/native.tests/testData/kt43502")
@TestDataPath("\$PROJECT_ROOT")
@Tag("frontend-fir")
@FirPipeline()
class FirStaticKT43502Test : KT43502TestBase()

abstract class KT43502TestBase : AbstractNativeSimpleTest() {
    private val testCompilationFactory = TestCompilationFactory()

    @Test
    fun test() {
        testRunSettings.assumeLibraryKindSupported()
        val rootDir = File("native/native.tests/testData/kt43502")

        val libFile = buildDir.resolve("kt43502.a")
        compileWithClangToStaticLibrary(sourceFiles = listOf(rootDir.resolve("kt43502.c")), outputFile = libFile).assertSuccess()
        val defFile = buildDir.resolve("kt43502.def").also {
            it.printWriter().use {
                it.println(
                    """
                    libraryPaths = ${buildDir.absolutePath.replace("\\", "/")}
                    staticLibraries = ${libFile.name}
                """.trimIndent()
                )
            }
        }
        val cinteropModule = TestModule.Exclusive("kt43502", emptySet(), emptySet(), emptySet())
        cinteropModule.files += TestFile.createCommitted(defFile, cinteropModule)
        val ktModule = TestModule.Exclusive("main", setOf("kt43502"), emptySet(), emptySet())
        ktModule.files += TestFile.createCommitted(rootDir.resolve("main.kt"), ktModule)

        val testCase = TestCase(
            id = TestCaseId.Named("kt43502"),
            kind = TestKind.STANDALONE_NO_TR,
            modules = setOf(cinteropModule, ktModule),
            freeCompilerArgs = TestCompilerArgs(
                compilerArgs = listOf(
                    "-opt-in", "kotlinx.cinterop.ExperimentalForeignApi",
                ), cinteropArgs = listOf("-header", rootDir.resolve("kt43502.h").absolutePath)
            ),
            nominalPackageName = PackageName("kt43502"),
            checks = TestRunChecks.Default(testRunSettings.get<Timeouts>().executionTimeout).run {
                copy(
                    outputDataFile = TestRunCheck.OutputDataFile(file = rootDir.resolve("main-main.out")),
                )
            },
            extras = TestCase.NoTestRunnerExtras(entryPoint = "main")
        ).apply {
            initialize(null, null)
        }

        val binaryLibrary = testCompilationFactory.testCaseToBinaryLibrary(
            testCase,
            testRunSettings,
            kind = testRunSettings.get<BinaryLibraryKind>(),
        ).result.assertSuccess().resultingArtifact

        val clangExecutableName = "clangMain"
        // We create executable in the same directory as dynamic library because there is no rpath on Windows.
        // Possible alternative: generate executable in buildDir and move or copy DLL there.
        // It might make sense in case of multiple dynamic libraries, but let's keep things simple for now.
        val executableFile = File(binaryLibrary.libraryFile.parentFile, clangExecutableName)
        val includeDirectories = binaryLibrary.headerFile?.let { listOf(it.parentFile) } ?: emptyList()
        val libraryName = binaryLibrary.libraryFile.nameWithoutExtension.substringAfter("lib")
        val clangResult = compileWithClang(
            sourceFiles = listOf(rootDir.resolve("main.c")),
            includeDirectories = includeDirectories,
            outputFile = executableFile,
            libraryDirectories = listOf(binaryLibrary.libraryFile.parentFile),
            libraries = listOf(libraryName),
            additionalClangFlags = testRunSettings.getKindSpecificClangFlags(binaryLibrary) + listOf("-Wall", "-Werror"),
        ).assertSuccess()

        val testExecutable = TestExecutable(
            clangResult.resultingArtifact,
            loggedCompilationToolCall = clangResult.loggedData,
            testNames = listOf(TestName("TMP")),
        )

        runExecutableAndVerify(testCase, testExecutable)
    }
}