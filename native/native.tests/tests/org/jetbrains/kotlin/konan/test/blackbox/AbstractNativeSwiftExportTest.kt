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
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunProvider
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.*
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.BinaryLibraryKind
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.Timeouts
import org.jetbrains.kotlin.konan.test.blackbox.support.util.DEFAULT_MODULE_NAME
import org.jetbrains.kotlin.konan.test.blackbox.support.util.ThreadSafeCache
import org.jetbrains.kotlin.konan.test.blackbox.support.util.createModuleMap
import org.jetbrains.kotlin.konan.test.blackbox.support.util.getAbsoluteFile
import org.jetbrains.kotlin.swiftexport.standalone.*
import org.jetbrains.kotlin.utils.KotlinNativePaths
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.extension.ExtendWith
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.div

@ExtendWith(SwiftExportTestSupport::class)
abstract class AbstractNativeSwiftExportTest {
    lateinit var testRunSettings: TestRunSettings
    lateinit var testRunProvider: TestRunProvider

    private val binariesDir get() = testRunSettings.get<Binaries>().testBinariesDir
    protected fun buildDir(testName: String) = binariesDir.resolve(testName)
    private val targets: KotlinNativeTargets get() = testRunSettings.get()
    private val testCompilationFactory = TestCompilationFactory()
    private val compiledSwiftCache = ThreadSafeCache<SwiftExportModule, TestCompilationArtifact.Swift.Module>()

    protected abstract fun runCompiledTest(
        testPathFull: File,
        testCase: TestCase,
        swiftExportOutput: SwiftExportModule,
        swiftModule: TestCompilationArtifact.Swift.Module,
    )

    protected abstract fun constructSwiftExportConfig(
        testPathFull: File,
    ): SwiftExportConfig

    protected fun runTest(@TestDataFile testDir: String) {
        Assumptions.assumeTrue(targets.testTarget.family.isAppleFamily)
        val testPathFull = getAbsoluteFile(testDir)

        val testCaseId = TestCaseId.TestDataFile((testPathFull.toPath() / "${testPathFull.name}.kt").toFile())
        val originalTestCase = testRunProvider.testCaseGroupProvider
            .getTestCaseGroup(testCaseId.testCaseGroupId, testRunSettings)
            ?.getByName(testCaseId)!!

        // run swift export
        val swiftExportOutput = runSwiftExport(
            originalTestCase.constructSwiftInput(),
            constructSwiftExportConfig(testPathFull)
        ).getOrThrow().first()

        // compile kotlin into binary
        val additionalKtFiles = mutableSetOf<Path>()
            .apply { swiftExportOutput.collectKotlinBridgeFilesRecursively(into = this) }

        val kotlinFiles = originalTestCase.modules.first().files.map { it.location }
        val resultingTestCase = generateSwiftExportTestCase(testPathFull.name, kotlinFiles + additionalKtFiles.map { it.toFile() })
        val kotlinBinaryLibrary = testCompilationFactory.testCaseToBinaryLibrary(
            resultingTestCase, testRunSettings,
            kind = BinaryLibraryKind.DYNAMIC,
        ).result.assertSuccess().resultingArtifact

        // compile swift into binary
        val swiftModule = swiftExportOutput.compile(
            compiledKotlinLibrary = kotlinBinaryLibrary,
            testPathFull,
        )

        // at this point we know that the generated code from SwiftExport can be compiled into library
        // and we are ready to perform other checks
        runCompiledTest(
            testPathFull,
            resultingTestCase,
            swiftExportOutput,
            swiftModule
        )
    }

    private fun TestCase.constructSwiftInput(): InputModule.Binary {
        val klib = testCompilationFactory
            .testCaseToKLib(this, testRunSettings)
            .result.assertSuccess().resultingArtifact
        return InputModule.Binary(
            path = Path(klib.path),
            name = modules.first().name
        )
    }

    private fun List<SwiftExportModule>.collectKotlinBridgeFilesRecursively(into: MutableSet<Path>) =
        forEach { module -> module.collectKotlinBridgeFilesRecursively(into) }

    private fun SwiftExportModule.collectKotlinBridgeFilesRecursively(into: MutableSet<Path>) {
        into.add(files.kotlinBridges)
        dependencies.collectKotlinBridgeFilesRecursively(into)
    }

    private fun SwiftExportModule.compile(
        compiledKotlinLibrary: TestCompilationArtifact.BinaryLibrary,
        testPathFull: File,
    ): TestCompilationArtifact.Swift.Module = compiledSwiftCache.computeIfAbsent(this) {
        val swiftModuleDir = buildDir(testPathFull.name).resolve("SwiftModules").also { it.mkdirs() }
        val bridgeModuleFile = createModuleMap(
            swiftModuleDir, files.cHeaderBridges.toFile()
        )
        val deps = dependencies.map { it.compile(compiledKotlinLibrary, testPathFull) }
        return@computeIfAbsent compileSwiftModule(
            swiftModuleDir = swiftModuleDir,
            swiftModuleName = name,
            sources = listOf(files.swiftApi.toFile()),
            kotlinBridgeModuleMap = bridgeModuleFile,
            binaryLibrary = compiledKotlinLibrary,
            deps = deps,
        )
    }

    private fun compileSwiftModule(
        swiftModuleDir: File,
        swiftModuleName: String,
        sources: List<File>,
        kotlinBridgeModuleMap: File,
        binaryLibrary: TestCompilationArtifact.BinaryLibrary,
        deps: List<TestCompilationArtifact.Swift.Module>,
    ): TestCompilationArtifact.Swift.Module {
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
