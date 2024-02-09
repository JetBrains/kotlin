/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.konan.test.blackbox.support.*
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact.KLIB
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.group.UsePartialLinkage
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunChecks
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeClassLoader
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.PipelineType
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.Timeouts
import org.jetbrains.kotlin.konan.test.blackbox.support.util.getAbsoluteFile
import org.jetbrains.kotlin.konan.test.blackbox.support.util.dumpMetadata
import org.jetbrains.kotlin.library.KotlinIrSignatureVersion
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertEqualsToFile
import org.jetbrains.kotlin.test.services.JUnit5Assertions.fail
import org.jetbrains.kotlin.test.utils.withSuffixAndExtension
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.junit.jupiter.api.Tag
import java.io.File

@Tag("klib")
@UsePartialLinkage(UsePartialLinkage.Mode.DISABLED)
abstract class AbstractNativeKlibDumpMetadataTest : AbstractNativeSimpleTest() {

    protected fun runTest(@TestDataFile testPath: String) {
        val testPathFull = getAbsoluteFile(testPath)
        muteTestIfNecessary(testPathFull)

        val testCase: TestCase = generateTestCaseWithSingleSource(testPathFull, listOf())
        val testCompilationResult: TestCompilationResult.Success<out KLIB> = compileToLibrary(testCase)

        val kotlinNativeClassLoader = testRunSettings.get<KotlinNativeClassLoader>()
        val klib: KLIB = testCompilationResult.assertSuccess().resultingArtifact

        val isFir = testRunSettings.get<PipelineType>() == PipelineType.K2
        val isFirIdentical = firIdentical(testPathFull)

        (KotlinIrSignatureVersion.CURRENTLY_SUPPORTED_VERSIONS + null).forEach { signatureVersion: KotlinIrSignatureVersion? ->
            val metadataDump = klib.dumpMetadata(
                kotlinNativeClassLoader.classLoader,
                printSignatures = signatureVersion != null,
                signatureVersion
            )

            val testDataFileK1 = testDataFile(testPathFull, signatureVersion, isFir = false)
            val testDataFileK2 = testDataFile(testPathFull, signatureVersion, isFir = true)

            checkTestDataFilesNotEqual(testPathFull, testDataFileK1, testDataFileK2)

            val testDataFile = if (isFir && !isFirIdentical) testDataFileK2 else testDataFileK1

            assertEqualsToFile(testDataFile, metadataDump)
        }
    }

    private fun testDataFile(testPathFull: File, signatureVersion: KotlinIrSignatureVersion?, isFir: Boolean): File {
        val versionSpecificSuffix = signatureVersion?.let { ".v${it.number}" }.orEmpty()
        val firSpecificSuffix = runIf(isFir) { ".fir" }.orEmpty()

        return testPathFull.withSuffixAndExtension(
            suffix = "$versionSpecificSuffix$firSpecificSuffix",
            extension = "txt"
        )
    }

    private fun generateTestCaseWithSingleSource(source: File, extraArgs: List<String>): TestCase {
        val moduleName: String = source.name
        val module = TestModule.Exclusive(moduleName, emptySet(), emptySet(), emptySet())
        module.files += TestFile.createCommitted(source, module)

        return TestCase(
            id = TestCaseId.Named(moduleName),
            kind = TestKind.STANDALONE,
            modules = setOf(module),
            freeCompilerArgs = TestCompilerArgs(extraArgs),
            nominalPackageName = PackageName.EMPTY,
            checks = TestRunChecks.Default(testRunSettings.get<Timeouts>().executionTimeout),
            extras = TestCase.WithTestRunnerExtras(TestRunnerType.DEFAULT)
        ).apply {
            initialize(null, null)
        }
    }

    private fun checkTestDataFilesNotEqual(kotlinTestDataFile: File, testDataFileK1: File, testDataFileK2: File) {
        if (testDataFileK1.exists() && testDataFileK2.exists()) {
            val originalText = testDataFileK1.readText().trimEnd()
            val firText = testDataFileK2.readText().trimEnd()

            val sameDumps = originalText == firText

            if (sameDumps) {
                testDataFileK2.delete()

                kotlinTestDataFile.writeText("// FIR_IDENTICAL\n" + kotlinTestDataFile.readText())

                fail {
                    """
                        Dump files are equal. Please re-run the test.
                        K1: ${testDataFileK1.absolutePath}
                        K2: ${testDataFileK2.absolutePath}
                    """.trimIndent()
                }
            }
        }
    }

}
