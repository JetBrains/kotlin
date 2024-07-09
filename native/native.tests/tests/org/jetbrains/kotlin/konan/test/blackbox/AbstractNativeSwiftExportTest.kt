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
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationFactory.ProduceStaticCache
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunCheck
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunChecks
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunProvider
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.*
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.BinaryLibraryKind
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.Timeouts
import org.jetbrains.kotlin.konan.test.blackbox.support.util.*
import org.jetbrains.kotlin.konan.test.blackbox.support.util.DEFAULT_MODULE_NAME
import org.jetbrains.kotlin.konan.test.blackbox.support.util.ThreadSafeCache
import org.jetbrains.kotlin.konan.test.blackbox.support.util.createModuleMap
import org.jetbrains.kotlin.konan.test.blackbox.support.util.getAbsoluteFile
import org.jetbrains.kotlin.sir.builder.buildModule
import org.jetbrains.kotlin.swiftexport.standalone.*
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertTrue
import org.jetbrains.kotlin.utils.KotlinNativePaths
import org.jetbrains.kotlin.utils.filterToSetOrEmpty
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
        swiftExportOutputs: Set<SwiftExportModule>,
        swiftModules: Set<TestCompilationArtifact.Swift.Module>,
        kotlinBinaryLibrary: TestCompilationArtifact.BinaryLibrary,
    )

    protected abstract fun constructSwiftExportConfig(
        module: TestModule.Exclusive,
    ): SwiftExportConfig

    protected fun runTest(@TestDataFile testDir: String) {
        Assumptions.assumeTrue(targets.testTarget.family.isAppleFamily)
        val testPathFull = getAbsoluteFile(testDir)

        val testCaseId = TestCaseId.TestDataFile((testPathFull.toPath() / "${testPathFull.name}.kt").toFile())
        val originalTestCase = testRunProvider.testCaseGroupProvider
            .getTestCaseGroup(testCaseId.testCaseGroupId, testRunSettings)
            ?.getByName(testCaseId)!!

        val rootModules = originalTestCase.rootModules
        val modulesMarkedForExport = originalTestCase.modules.filterToSetOrEmpty { it.shouldBeExportedToSwift() }
        val modulesToExport = rootModules + modulesMarkedForExport

        // run swift export

        // this code will be moved into swift-export-standalone during KT-68864. currently this is a hack
        // ATTENTION:
        // 1. Each call to `runSwiftExport` will end with a write operation of contents of this module onto file-system.
        // 2. Each call to `runSwiftExport` will modify this module AND there is no synchronization mechanism inplace.
        val moduleForPackages = buildModule {
            name = "ExportedKotlinPackages"
        }
        val swiftExportOutputs = modulesToExport.flatMapToSet { rootModule ->
            val (swiftExportInput, klibDeps) = rootModule.constructSwiftInput(originalTestCase.freeCompilerArgs)
            runSwiftExport(
                swiftExportInput,
                klibDeps,
                moduleForPackages,
                constructSwiftExportConfig(rootModule)
            ).getOrThrow()
        }

        // patch references (will be moved into runner during KT-68864)
        swiftExportOutputs.forEach { realModule ->
            realModule.dependencies.forEach { dep ->
                dep.module = swiftExportOutputs.first { it.name == dep.name }
            }
        }


        // compile kotlin into binary
        val additionalKtFiles: Set<Path> = mutableSetOf<Path>()
            .apply { swiftExportOutputs.collectKotlinBridgeFilesRecursively(into = this) }

        val kotlinFiles = modulesToExport.flatMapToSet { it.files.map { it.location } }
        val kotlinBinaryLibraryName = testPathFull.name + "Kotlin"

        val resultingTestCase = generateSwiftExportTestCase(
            testPathFull,
            kotlinBinaryLibraryName,
            kotlinFiles.toList() + additionalKtFiles.map { it.toFile() },
            dependencies = modulesToExport
                .flatMapToSet {
                    it.allRegularDependencies.filterIsInstance<TestModule.Exclusive>().toSet()
                } - modulesToExport,
        )

        val kotlinBinaryLibrary = testCompilationFactory.testCaseToBinaryLibrary(
            resultingTestCase, testRunSettings,
            kind = BinaryLibraryKind.DYNAMIC,
        ).result.assertSuccess().resultingArtifact

        // compile swift into binary
        val swiftModules = swiftExportOutputs.flatMapToSet {
            it.compile(
                compiledKotlinLibrary = kotlinBinaryLibrary,
                testPathFull,
            )
        }

        // at this point we know that the generated code from SwiftExport can be compiled into library
        // and we are ready to perform other checks
        runCompiledTest(
            testPathFull,
            resultingTestCase,
            swiftExportOutputs,
            swiftModules,
            kotlinBinaryLibrary
        )
    }

    private data class SwiftInputModules(
        val moduleToTranslate: InputModule.Binary,
        val dependencies: List<InputModule.Binary>
    )

    private fun TestModule.Exclusive.constructSwiftInput(freeCompilerArgs: TestCompilerArgs): SwiftInputModules {
        val moduleToTranslate = this
        val klibToTranslate = testCompilationFactory.modulesToKlib(
            sourceModules = setOf(moduleToTranslate),
            freeCompilerArgs = freeCompilerArgs,
            settings = testRunSettings,
            produceStaticCache = ProduceStaticCache.No,
        )
        return SwiftInputModules(
            moduleToTranslate = InputModule.Binary(
                path = Path(klibToTranslate.klib.result.assertSuccess().resultingArtifact.path),
                name = moduleToTranslate.name
            ),
            dependencies = moduleToTranslate.allRegularDependencies.map {
                val klib = testCompilationFactory.modulesToKlib(
                    sourceModules = setOf(it),
                    freeCompilerArgs = freeCompilerArgs,
                    settings = testRunSettings,
                    produceStaticCache = ProduceStaticCache.No,
                ).klib.result.assertSuccess().resultingArtifact
                InputModule.Binary(
                    path = Path(klib.path),
                    name = it.name
                )
            }
        )
    }

    private fun Collection<SwiftExportModule>.collectKotlinBridgeFilesRecursively(into: MutableSet<Path>) =
        forEach { module -> module.collectKotlinBridgeFilesRecursively(into) }

    private fun SwiftExportModule.collectKotlinBridgeFilesRecursively(into: MutableSet<Path>) {
        if (this is SwiftExportModule.BridgesToKotlin) into.add(files.kotlinBridges)
        dependencies.filterIsInstance<SwiftExportModule.BridgesToKotlin>().collectKotlinBridgeFilesRecursively(into)
    }

    private fun SwiftExportModule.compile(
        compiledKotlinLibrary: TestCompilationArtifact.BinaryLibrary,
        testPathFull: File,
    ): Set<TestCompilationArtifact.Swift.Module> {
        val deps = resolvedDependencies.flatMapToSet { it.compile(compiledKotlinLibrary, testPathFull) }
        val compiledSwiftModule = when (this) {
            is SwiftExportModule.BridgesToKotlin -> compile(compiledKotlinLibrary, testPathFull)
            is SwiftExportModule.SwiftOnly -> compile(compiledKotlinLibrary, testPathFull)
        }
        return deps + compiledSwiftModule
    }

    private fun SwiftExportModule.SwiftOnly.compile(
        compiledKotlinLibrary: TestCompilationArtifact.BinaryLibrary,
        testPathFull: File,
    ): Set<TestCompilationArtifact.Swift.Module> {
        val compiledSwiftModule = compiledSwiftCache.computeIfAbsent(this) {
            val swiftModuleDir = buildDir(testPathFull.name).resolve("SwiftModules").also { it.mkdirs() }
            return@computeIfAbsent compileSwiftModule(
                swiftModuleDir = swiftModuleDir,
                swiftModuleName = name,
                sources = listOf(swiftApi.toFile()),
                kotlinBridgeModuleMap = null,
                binaryLibrary = compiledKotlinLibrary,
                deps = emptyList(),
            )
        }
        return setOf(compiledSwiftModule)
    }

    private fun SwiftExportModule.BridgesToKotlin.compile(
        compiledKotlinLibrary: TestCompilationArtifact.BinaryLibrary,
        testPathFull: File,
    ): Set<TestCompilationArtifact.Swift.Module> {
        val deps = resolvedDependencies.flatMapToSet { it.compile(compiledKotlinLibrary, testPathFull) }
        val compiledSwiftModule = compiledSwiftCache.computeIfAbsent(this) {
            it as SwiftExportModule.BridgesToKotlin
            val swiftModuleDir = buildDir(testPathFull.name).resolve("SwiftModules").also { it.mkdirs() }
            val umbrellaHeader = files.cHeaderBridges.toFile()
            val bridgeModuleFile = createModuleMap(
                moduleName = it.bridgeName,
                directory = files.cHeaderBridges.toFile().parentFile,
                umbrellaHeader = umbrellaHeader,
            )
            return@computeIfAbsent compileSwiftModule(
                swiftModuleDir = swiftModuleDir,
                swiftModuleName = name,
                sources = listOf(files.swiftApi.toFile()),
                kotlinBridgeModuleMap = bridgeModuleFile,
                binaryLibrary = compiledKotlinLibrary,
                deps = deps,
            )
        }
        return deps + compiledSwiftModule
    }

    private fun compileSwiftModule(
        swiftModuleDir: File,
        swiftModuleName: String,
        sources: List<File>,
        kotlinBridgeModuleMap: File?,
        binaryLibrary: TestCompilationArtifact.BinaryLibrary,
        deps: Collection<TestCompilationArtifact.Swift.Module>,
    ): TestCompilationArtifact.Swift.Module {
        val binaryLibraryName = binaryLibrary.libraryFile.nameWithoutExtension.substringAfter("lib")
        return SwiftCompilation(
            testRunSettings = testRunSettings,
            sources = sources,
            expectedArtifact = TestCompilationArtifact.Swift.Module(
                rootDir = swiftModuleDir,
                moduleName = swiftModuleName,
            ),
            swiftExtraOpts = listOf(
                *(modulemapFileToSwiftCompilerOptionsIfNeeded(kotlinBridgeModuleMap)).toTypedArray(),
                "-Xcc", "-fmodule-map-file=${Distribution(KotlinNativePaths.homePath.absolutePath).kotlinRuntimeForSwiftModuleMap}",
                "-L", binaryLibrary.libraryFile.parentFile.absolutePath,
                "-l$binaryLibraryName",
                *deps.flatMap { dependency ->
                    listOf(
                        "-L", dependency.binaryLibrary.parentFile.absolutePath,
                        "-I", dependency.binaryLibrary.parentFile.absolutePath,
                        "-l${dependency.moduleName}",
                    )
                }.toTypedArray(),
                "-emit-module", "-parse-as-library", "-emit-library", "-enable-library-evolution",
                "-module-name", swiftModuleName,
            ),
            outputFile = { it.binaryLibrary },
        ).result.assertSuccess().resultingArtifact
    }

    private fun generateSwiftExportTestCase(
        testPathFull: File,
        testName: String = testPathFull.name,
        sources: List<File>,
        dependencies: Set<TestModule.Exclusive>,
    ): TestCase {
        val module = TestModule.Exclusive(DEFAULT_MODULE_NAME, emptySet(), emptySet(), emptySet())
        sources.forEach { module.files += TestFile.createCommitted(it, module) }

        val regexes = testPathFull.list()!!
            .singleOrNull { it.endsWith(".out.re") }
            ?.let { testPathFull.resolve(it) }

        val exitCode = testPathFull.list()!!
            .singleOrNull { it == "exitCode" }
            ?.let { testPathFull.resolve(it).readText() }

        return TestCase(
            id = TestCaseId.Named(testName),
            kind = TestKind.STANDALONE_NO_TR,
            modules = setOf(module) + dependencies,
            freeCompilerArgs = TestCompilerArgs(
                listOf(
                    "-opt-in", "kotlin.experimental.ExperimentalNativeApi",
                    "-opt-in", "kotlinx.cinterop.ExperimentalForeignApi",
                    "-opt-in", "kotlin.native.internal.InternalForKotlinNative", // for uninitialized object instance manipulation, and ExternalRCRef.
                    "-Xbinary=swiftExport=true",
                )
            ),
            nominalPackageName = PackageName(testName),
            checks = TestRunChecks.Default(testRunSettings.get<Timeouts>().executionTimeout).run {
                copy(
                    outputMatcher = regexes?.let { regexesFile ->
                        val localRegexes = regexesFile.readLines().map { it.toRegex(RegexOption.DOT_MATCHES_ALL) }
                        TestRunCheck.OutputMatcher {
                            localRegexes.forEach { regex ->
                                assertTrue(regex.matches(it)) {
                                    "Regex `$regex` failed to match `$it`"
                                }
                            }
                            true
                        }
                    },
                    exitCodeCheck = exitCode?.let {
                        if (it == "!0") {
                            TestRunCheck.ExitCode.AnyNonZero
                        } else {
                            TestRunCheck.ExitCode.Expected(it.toInt())
                        }
                    } ?: exitCodeCheck
                )
            },
            extras = TestCase.NoTestRunnerExtras(entryPoint = "main")
        ).apply {
            initialize(null, null)
        }
    }
}

private fun modulemapFileToSwiftCompilerOptionsIfNeeded(modulemap: File?) = modulemap?.let {
    listOf(
        "-Xcc", "-fmodule-map-file=${it.absolutePath}",
    )
} ?: emptyList()

private val SwiftExportModule.resolvedDependencies: List<SwiftExportModule>
    get() = dependencies.map(SwiftExportModule.Reference::module)
