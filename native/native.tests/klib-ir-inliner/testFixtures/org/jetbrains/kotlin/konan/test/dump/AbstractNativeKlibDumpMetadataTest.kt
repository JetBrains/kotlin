/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.dump

import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.konan.test.blackbox.AbstractNativeSimpleTest
import org.jetbrains.kotlin.konan.test.blackbox.compileToLibrary
import org.jetbrains.kotlin.konan.test.blackbox.muteTestIfNecessary
import org.jetbrains.kotlin.konan.test.blackbox.support.*
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact.KLIB
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.group.UsePartialLinkage
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunChecks
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeClassLoader
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.Timeouts
import org.jetbrains.kotlin.konan.test.blackbox.support.util.dumpMetadata
import org.jetbrains.kotlin.konan.test.blackbox.support.util.getAbsoluteFile
import org.jetbrains.kotlin.test.services.JUnit5Assertions
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertEqualsToFile
import org.jetbrains.kotlin.test.services.JUnit5Assertions.fail
import org.jetbrains.kotlin.test.services.impl.ModuleStructureExtractorImpl
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.jetbrains.kotlin.test.utils.withSuffixAndExtension
import org.junit.jupiter.api.Tag
import java.io.File

@Tag("klib")
@UsePartialLinkage(UsePartialLinkage.Mode.ERROR)
abstract class AbstractNativeKlibDumpMetadataTest : AbstractNativeSimpleTest() {

    protected fun runTest(@TestDataFile testPath: String) {
        val testPathFull = getAbsoluteFile(testPath)
        muteTestIfNecessary(testPathFull)

        val testCase: TestCase = generateTestCaseWithSingleSource(testPathFull, listOf())
        val testCompilationResult: TestCompilationResult.Success<out KLIB> = compileToLibrary(testCase)

        val kotlinNativeClassLoader = testRunSettings.get<KotlinNativeClassLoader>()
        val klib: KLIB = testCompilationResult.assertSuccess().resultingArtifact

        JUnit5Assertions.assertAll(
            supportedDumpModes.map { dumpMode ->
                {
                    val metadataDump = klib.dumpMetadata(kotlinNativeClassLoader.classLoader, metadataTestMode = dumpMode)

                    val testDataFile = testPathFull.withSuffixAndExtension(
                        suffix = dumpMode?.let { ".$it" }.orEmpty(),
                        extension = "txt"
                    )

                    assertEqualsToFile(testDataFile, metadataDump)
                }
            }
        )
    }

    private fun generateTestCaseWithSingleSource(source: File, extraArgs: List<String>): TestCase {
        val moduleStructure = ModuleStructureExtractorImpl.parseModuleStructureWithoutService(source, JUnit5Assertions)
        if (moduleStructure.modules.size > 1) {
            fail { "Test should contain only one module" }
        }
        val moduleName: String = source.name
        val module = TestModule.Exclusive(
            moduleName,
            directRegularDependencySymbols = emptySet(),
            directFriendDependencySymbols = emptySet(),
            directDependsOnDependencySymbols = emptySet()
        )
        val rootDir = KtTestUtil.tmpDirForTest(this::class.java.name, source.nameWithoutExtension)
        for (testFile in moduleStructure.modules.single().files) {
            val realFile = rootDir.resolve(testFile.relativePath).also {
                it.parentFile.mkdirs()
                it.writeText(testFile.originalContent.trimStart())
            }
            module.files += TestFile.createCommitted(realFile, module)
        }

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

    companion object {
        private val supportedDumpModes = listOf(null, "compact-with-stable-order", "ultracompact-with-stable-order")
    }
}
