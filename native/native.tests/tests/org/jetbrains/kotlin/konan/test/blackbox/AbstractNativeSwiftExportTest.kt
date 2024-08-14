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

        val input = modulesToExport.mapToSet {
            it.constructSwiftInput(
                originalTestCase.freeCompilerArgs,
                constructSwiftExportConfig(it)
            )
        }

        // run swift export
        val swiftExportOutputs: Set<SwiftExportModule> = runSwiftExport(
            input
        ).getOrThrow()

        // compile kotlin into binary
        val additionalKtFiles: Set<Path> = mutableSetOf<Path>()
            .apply { swiftExportOutputs.collectKotlinBridgeFilesRecursively(into = this) }

        val kotlinFiles = modulesToExport.flatMapToSet { it.files.map { it.location } }
        val kotlinBinaryLibraryName = testPathFull.name + "Kotlin"

        val typeMappings = swiftExportOutputs.mapNotNull {
            (it as? SwiftExportModule.BridgesToKotlin)?.files?.typeMappings?.toFile()
        }

        val resultingTestCase = generateSwiftExportTestCase(
            testPathFull,
            kotlinBinaryLibraryName,
            kotlinFiles.toList() + additionalKtFiles.map { it.toFile() },
            dependencies = modulesToExport
                .flatMapToSet {
                    it.allRegularDependencies.filterIsInstance<TestModule.Exclusive>().toSet()
                } - modulesToExport,
            typeMappings = typeMappings,
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
                swiftExportOutputs
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

    private fun TestModule.Exclusive.constructSwiftInput(
        freeCompilerArgs: TestCompilerArgs,
        config: SwiftExportConfig,
    ): InputModule {
        val moduleToTranslate = this
        val klibToTranslate = testCompilationFactory.modulesToKlib(
            sourceModules = setOf(moduleToTranslate),
            freeCompilerArgs = freeCompilerArgs,
            settings = testRunSettings,
            produceStaticCache = ProduceStaticCache.No,
        )
        return InputModule(
            path = Path(klibToTranslate.klib.result.assertSuccess().resultingArtifact.path),
            name = moduleToTranslate.name,
            config = config,
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
        allModules: Set<SwiftExportModule>,
    ): Set<TestCompilationArtifact.Swift.Module> {
        val deps = resolvedDependencies(allModules).flatMapToSet { it.compile(compiledKotlinLibrary, testPathFull, allModules) }
        val compiledSwiftModule = when (this) {
            is SwiftExportModule.BridgesToKotlin -> compile(compiledKotlinLibrary, testPathFull, allModules)
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
        allModules: Set<SwiftExportModule>,
    ): Set<TestCompilationArtifact.Swift.Module> {
        val deps = resolvedDependencies(allModules).flatMapToSet { it.compile(compiledKotlinLibrary, testPathFull, allModules) }
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
        typeMappings: List<File>,
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
                    "-Xbinary=swiftExportTypeMappings=${typeMappings.joinToString(separator = ":") { it.absolutePath }}"
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

private fun SwiftExportModule.resolvedDependencies(allModules: Set<SwiftExportModule>): List<SwiftExportModule> = dependencies.map { dep ->
    allModules.firstOrNull { it.name == dep.name } ?: error("Module ${this.name} requested non-existing dependency ${dep.name}")
}
