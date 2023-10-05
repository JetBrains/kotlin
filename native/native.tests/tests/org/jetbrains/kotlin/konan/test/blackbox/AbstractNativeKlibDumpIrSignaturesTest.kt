/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.konan.test.blackbox.support.*
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationDependency
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunChecks
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeClassLoader
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.PipelineType
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.Timeouts
import org.jetbrains.kotlin.konan.test.blackbox.support.util.getAbsoluteFile
import org.jetbrains.kotlin.konan.test.blackbox.support.util.dumpIrSignatures
import org.jetbrains.kotlin.library.KotlinIrSignatureVersion
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertEqualsToFile
import org.jetbrains.kotlin.test.utils.withExtension
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import java.io.File

@Tag("klib")
abstract class AbstractNativeKlibDumpIrSignaturesTest : AbstractNativeSimpleTest() {
    protected fun runTest(@TestDataFile testDataPath: String) {
        val testDataFile = getAbsoluteFile(testDataPath)
        muteTestIfNecessary(testDataFile)

        val dependencies: List<TestCompilationDependency<TestCompilationArtifact.KLIB>> = buildList {
            this += compileRegularDependencies(testDataFile)
            this += compileInteropDependencies(testDataFile)
        }

        val library = compileSingleFileToLibrary(testDataFile, dependencies)

        val testPathNoExtension = testDataFile.canonicalPath.substringBeforeLast(".")

        val firSpecificExt =
            if (testRunSettings.get<PipelineType>() == PipelineType.K2 && !firIdentical(testDataFile))
                ".fir"
            else ""

        KotlinIrSignatureVersion.CURRENTLY_SUPPORTED_VERSIONS.forEach { signatureVersion ->
            val expectedContents = File("$testPathNoExtension$firSpecificExt.ir-signatures.v${signatureVersion.number}.txt")
            assertSignaturesMatchExpected(library, expectedContents, signatureVersion)
        }

    }

    private fun assertSignaturesMatchExpected(
        library: TestCompilationArtifact.KLIB,
        expectedContents: File,
        signatureVersion: KotlinIrSignatureVersion
    ) {
        val kotlinNativeClassLoader = testRunSettings.get<KotlinNativeClassLoader>()
        val dumpedSignatures = library.dumpIrSignatures(kotlinNativeClassLoader.classLoader, signatureVersion)
        assertEqualsToFile(expectedContents, dumpedSignatures)
    }

    private fun compileInteropDependencies(testDataFile: File): List<TestCompilationDependency<TestCompilationArtifact.KLIB>> {
        val defFile = testDataFile.withExtension(".lib.def")
        if (!defFile.exists()) return emptyList()

        assertTrue(defFile.isFile) { "Def file does not exist: $defFile" }

        return listOf(
            cinteropToLibrary(
                targets = targets,
                defFile = defFile,
                outputDir = buildDir,
                freeCompilerArgs = TestCompilerArgs.EMPTY
            ).assertSuccess().resultingArtifact.asLibraryDependency()
        )
    }

    private fun compileRegularDependencies(testDataFile: File): List<TestCompilationDependency<TestCompilationArtifact.KLIB>> {
        val mainSourceFileName = testDataFile.name

        return testDataFile.parentFile.listFiles().orEmpty().mapNotNull { dependencyTestDataFile ->
            if (dependencyTestDataFile == testDataFile || !dependencyTestDataFile.isFile) return@mapNotNull null
            val dependencyTestDataFileName = dependencyTestDataFile.name

            val dependencyName = dependencyTestDataFileName.substringAfter(mainSourceFileName + "_", missingDelimiterValue = "")
            if (dependencyName.isBlank()) return@mapNotNull null

            val dependencySourceFile = dependencyTestDataFile.copyTo(buildDir.resolve("$dependencyName.kt"))
            compileSingleFileToLibrary(dependencySourceFile, emptyList()).asLibraryDependency()
        }
    }

    private fun compileSingleFileToLibrary(
        sourceFile: File,
        dependencies: List<TestCompilationDependency<TestCompilationArtifact.KLIB>>,
    ): TestCompilationArtifact.KLIB {
        val moduleName: String = sourceFile.nameWithoutExtension
        val module = TestModule.Exclusive(moduleName, emptySet(), emptySet(), emptySet())
        module.files += TestFile.createCommitted(sourceFile, module)

        val testCase = TestCase(
            id = TestCaseId.Named(moduleName),
            kind = TestKind.STANDALONE,
            modules = setOf(module),
            freeCompilerArgs = TestCompilerArgs("-Xklib-relative-path-base=${sourceFile.parent}"),
            nominalPackageName = PackageName.EMPTY,
            checks = TestRunChecks.Default(testRunSettings.get<Timeouts>().executionTimeout),
            extras = TestCase.WithTestRunnerExtras(TestRunnerType.DEFAULT)
        ).apply {
            initialize(null, null)
        }

        return compileToLibrary(testCase, *dependencies.toTypedArray()).assertSuccess().resultingArtifact
    }
}
