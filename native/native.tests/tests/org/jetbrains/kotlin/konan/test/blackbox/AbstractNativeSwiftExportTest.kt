/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.konan.test.blackbox.support.*
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.SwiftCompilation
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationFactory
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunChecks
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.BinaryLibraryKind
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.Timeouts
import org.jetbrains.kotlin.konan.test.blackbox.support.util.DEFAULT_MODULE_NAME
import org.jetbrains.kotlin.konan.test.blackbox.support.util.ThreadSafeCache
import org.jetbrains.kotlin.konan.test.blackbox.support.util.createModuleMap
import org.jetbrains.kotlin.konan.test.blackbox.support.util.getAbsoluteFile
import org.jetbrains.kotlin.swiftexport.standalone.*
import org.jetbrains.kotlin.utils.KotlinNativePaths
import org.junit.jupiter.api.Assumptions
import java.io.File
import java.nio.file.Path

abstract class AbstractNativeSwiftExportTest : AbstractNativeSimpleTest() {
    private val testCompilationFactory = TestCompilationFactory()
    private val compiledSwiftCache = ThreadSafeCache<SwiftExportModule, TestCompilationArtifact.Swift.Module>()

    protected abstract fun runCompiledTest(
        testPathFull: File,
        testCase: TestCase,
        swiftExportOutput: SwiftExportModule,
        swiftModule: TestCompilationArtifact.Swift.Module,
        kotlinBinaryLibrary: TestCompilationArtifact.BinaryLibrary,
    )

    protected abstract fun constructSwiftInput(
        testPathFull: File,
    ): InputModule

    protected abstract fun constructSwiftExportConfig(
        testPathFull: File,
    ): SwiftExportConfig

    protected abstract fun collectKotlinFiles(
        testPathFull: File,
    ): List<File>

    protected fun runTest(@TestDataFile testDir: String) {
        Assumptions.assumeTrue(targets.testTarget.family.isAppleFamily)
        val testPathFull = getAbsoluteFile(testDir)

        val swiftExportOutput = runSwiftExport(testPathFull)

        // compile kotlin into binary
        val additionalKtFiles = swiftExportOutput.collectKotlinBridgeFilesRecursively()
        val kotlinFiles = collectKotlinFiles(testPathFull)
        //
        val kotlinBinaryLibraryName = testPathFull.name + "Kotlin"
        val testCase = generateSwiftExportTestCase(kotlinBinaryLibraryName, kotlinFiles + additionalKtFiles.map { it.toFile() })
        val kotlinBinaryLibrary = testCompilationFactory.testCaseToBinaryLibrary(
            testCase, testRunSettings,
            kind = BinaryLibraryKind.DYNAMIC,
        ).result.assertSuccess().resultingArtifact

        // compile swift into binary
        val swiftModule = swiftExportOutput.compile(
            compiledKotlinLibrary = kotlinBinaryLibrary,
        )

        // at this point we know that the generated code from SwiftExport can be compiled into library
        // and we are ready to perform other checks
        runCompiledTest(
            testPathFull,
            testCase,
            swiftExportOutput,
            swiftModule,
            kotlinBinaryLibrary
        )
    }

    private fun List<SwiftExportModule>.collectKotlinBridgeFilesRecursively(): Collection<Path> =
        flatMapTo(mutableSetOf()) { module -> module.collectKotlinBridgeFilesRecursively() }

    private fun SwiftExportModule.collectKotlinBridgeFilesRecursively(): Collection<Path> =
        mutableSetOf(files.kotlinBridges) + dependencies.collectKotlinBridgeFilesRecursively()

    private fun SwiftExportModule.compile(
        compiledKotlinLibrary: TestCompilationArtifact.BinaryLibrary,
    ): TestCompilationArtifact.Swift.Module = compiledSwiftCache.computeIfAbsent(this) {
        val bridgeModuleFile = createModuleMap(
            buildDir, files.cHeaderBridges.toFile()
        )
        val deps = dependencies.map { it.compile(compiledKotlinLibrary) }
        return@computeIfAbsent compileSwiftModule(
            swiftModuleName = name,
            sources = listOf(files.swiftApi.toFile()),
            kotlinBridgeModuleMap = bridgeModuleFile,
            binaryLibrary = compiledKotlinLibrary,
            deps = deps,
        )
    }

    private fun compileSwiftModule(
        swiftModuleName: String,
        sources: List<File>,
        kotlinBridgeModuleMap: File,
        binaryLibrary: TestCompilationArtifact.BinaryLibrary,
        deps: List<TestCompilationArtifact.Swift.Module>,
    ): TestCompilationArtifact.Swift.Module {
        val swiftModuleDir = buildDir.resolve("SwiftModules").resolve(swiftModuleName).also { it.mkdirs() }
        val binaryLibraryName = binaryLibrary.libraryFile.nameWithoutExtension.substringAfter("lib")
        return SwiftCompilation(
            testRunSettings = testRunSettings,
            sources = sources,
            expectedArtifact = TestCompilationArtifact.Swift.Module(
                rootDir = swiftModuleDir,
                moduleName = swiftModuleName,
                modulemap = kotlinBridgeModuleMap
            ),
            swiftExtraOpts = listOf(
                "-Xcc", "-fmodule-map-file=${kotlinBridgeModuleMap.absolutePath}",
                "-Xcc", "-fmodule-map-file=${Distribution(KotlinNativePaths.homePath.absolutePath).kotlinRuntimeForSwiftModuleMap}",
                "-L", binaryLibrary.libraryFile.parentFile.absolutePath,
                "-l$binaryLibraryName",
                *deps.flatMap { dependency ->
                    listOf(
                        "-Xcc", "-fmodule-map-file=${dependency.modulemap.absolutePath}",
                        "-L", dependency.binaryLibrary.parentFile.absolutePath,
                    )
                }.toTypedArray(),
                "-emit-module", "-parse-as-library", "-emit-library", "-enable-library-evolution",
                "-module-name", swiftModuleName,
            ),
            outputFile = { it.binaryLibrary },
        ).result.assertSuccess().resultingArtifact
    }

    private fun runSwiftExport(
        testPathFull: File
    ): SwiftExportModule {
        val swiftExportInput = constructSwiftInput(testPathFull)
        val swiftExportConfig = constructSwiftExportConfig(testPathFull)

        return runSwiftExport(
            swiftExportInput,
            swiftExportConfig
        ).getOrThrow().first()
    }

    private fun generateSwiftExportTestCase(testName: String, sources: List<File>): TestCase {
        val module = TestModule.Exclusive(DEFAULT_MODULE_NAME, emptySet(), emptySet(), emptySet())
        sources.forEach { module.files += TestFile.createCommitted(it, module) }

        return TestCase(
            id = TestCaseId.Named(testName),
            kind = TestKind.STANDALONE_NO_TR,
            modules = setOf(module),
            freeCompilerArgs = TestCompilerArgs(
                listOf(
                    "-opt-in", "kotlin.experimental.ExperimentalNativeApi",
                    "-opt-in", "kotlinx.cinterop.ExperimentalForeignApi",
                    "-opt-in", "kotlin.native.internal.InternalForKotlinNative", // for uninitialized object instance manipulation, and ExternalRCRef.
                    "-Xbinary=swiftExport=true",
                )
            ),
            nominalPackageName = PackageName(testName),
            checks = TestRunChecks.Default(testRunSettings.get<Timeouts>().executionTimeout),
            extras = TestCase.NoTestRunnerExtras(entryPoint = "main")
        ).apply {
            initialize(null, null)
        }
    }
}
