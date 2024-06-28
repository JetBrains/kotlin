/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.konan.test.blackbox.support.*
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact.KLIB
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationDependency
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunChecks
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeClassLoader
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.PipelineType
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.Timeouts
import org.jetbrains.kotlin.konan.test.blackbox.support.util.getAbsoluteFile
import org.jetbrains.kotlin.konan.test.blackbox.support.util.dumpIrSignatures
import org.jetbrains.kotlin.konan.test.blackbox.support.util.dumpMetadataSignatures
import org.jetbrains.kotlin.library.KotlinIrSignatureVersion
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertEqualsToFile
import org.jetbrains.kotlin.test.utils.withExtension
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import java.io.File

@Tag("klib")
abstract class AbstractNativeKlibDumpSignaturesTest : AbstractNativeSimpleTest() {
    protected fun runTest(@TestDataFile testDataPath: String) {
        val testDataFile = getAbsoluteFile(testDataPath)
        muteTestIfNecessary(testDataFile)

        val library = buildLibrary(testDataFile)

        val testPathNoExtension = testDataFile.canonicalPath.substringBeforeLast(".")

        val firSpecificExt = if (testRunSettings.get<PipelineType>() == PipelineType.K2 && !firIdentical(testDataFile)) ".fir" else ""

        KotlinIrSignatureVersion.CURRENTLY_SUPPORTED_VERSIONS.forEach { signatureVersion ->
            val expectedContents = File("$testPathNoExtension$firSpecificExt.$suffix.v${signatureVersion.number}.txt")
            assertSignaturesMatchExpected(library, expectedContents, signatureVersion)
        }
    }

    internal abstract val suffix: String
    internal abstract fun buildLibrary(testDataFile: File): KLIB
    internal abstract fun dumpSignatures(library: KLIB, signatureVersion: KotlinIrSignatureVersion): String

    private fun assertSignaturesMatchExpected(
        library: KLIB,
        expectedContents: File,
        signatureVersion: KotlinIrSignatureVersion,
    ) {
        val dumpedSignatures = dumpSignatures(library, signatureVersion)
        assertEqualsToFile(expectedContents, dumpedSignatures)
    }

    internal fun compileRegularDependencies(testDataFile: File): List<TestCompilationDependency<KLIB>> {
        val mainSourceFileName = testDataFile.name

        return testDataFile.parentFile.listFiles().orEmpty().mapNotNull { dependencyTestDataFile ->
            if (dependencyTestDataFile == testDataFile || !dependencyTestDataFile.isFile) return@mapNotNull null
            val dependencyTestDataFileName = dependencyTestDataFile.name

            val dependencyName = dependencyTestDataFileName.substringAfter(mainSourceFileName + "_", missingDelimiterValue = "")
            if (dependencyName.isBlank()) return@mapNotNull null

            val dependencySourceFile = dependencyTestDataFile.copyTo(buildDir.resolve("$dependencyName.kt"))
            compileSingleKotlinFileToLibrary(dependencySourceFile, emptyList()).asLibraryDependency()
        }
    }

    internal fun compileCInteropDependencies(testDataFile: File): List<TestCompilationDependency<KLIB>> {
        val defFile = testDataFile.withExtension(".lib.def")
        if (!defFile.exists()) return emptyList()

        assertTrue(defFile.isFile) { "Def file does not exist: $defFile" }

        return listOf(compileDefFileToLibrary(defFile).asLibraryDependency())
    }

    internal fun compileSingleKotlinFileToLibrary(
        sourceFile: File,
        dependencies: List<TestCompilationDependency<KLIB>>,
    ): KLIB {
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

    internal fun compileDefFileToLibrary(defFile: File): KLIB {
        muteCInteropTestIfNecessary(defFile, targets.testTarget)

        return cinteropToLibrary(
            targets = targets,
            defFile = defFile,
            outputDir = buildDir,
            freeCompilerArgs = TestCompilerArgs.EMPTY
        ).assertSuccess().resultingArtifact
    }
}

abstract class AbstractNativeKlibDumpIrSignaturesTest : AbstractNativeKlibDumpSignaturesTest() {
    override val suffix get() = "ir-signatures"

    override fun buildLibrary(testDataFile: File) = compileSingleKotlinFileToLibrary(
        sourceFile = testDataFile,
        dependencies = compileRegularDependencies(testDataFile) + compileCInteropDependencies(testDataFile)
    )

    override fun dumpSignatures(library: KLIB, signatureVersion: KotlinIrSignatureVersion) =
        library.dumpIrSignatures(
            testRunSettings.get<KotlinNativeClassLoader>().classLoader,
            signatureVersion
        )
}

abstract class AbstractNativeKlibDumpMetadataSignaturesTest : AbstractNativeKlibDumpSignaturesTest() {
    override val suffix get() = "metadata-signatures"

    override fun buildLibrary(testDataFile: File) = when (testDataFile.extension) {
        "kt" -> compileSingleKotlinFileToLibrary(
            sourceFile = testDataFile,
            dependencies = compileRegularDependencies(testDataFile) + compileCInteropDependencies(testDataFile)
        )
        "def" -> compileDefFileToLibrary(testDataFile)
        else -> error("Unexpected test data file: $testDataFile")
    }

    override fun dumpSignatures(library: KLIB, signatureVersion: KotlinIrSignatureVersion) =
        library.dumpMetadataSignatures(
            testRunSettings.get<KotlinNativeClassLoader>().classLoader,
            signatureVersion
        ).lineSequence().filter(String::isNotBlank).sorted().joinToString("\n", postfix = "\n")
}
