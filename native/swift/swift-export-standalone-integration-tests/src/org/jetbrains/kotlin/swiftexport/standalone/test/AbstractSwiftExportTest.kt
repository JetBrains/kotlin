/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone.test

import com.intellij.openapi.util.io.FileUtil
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
import org.jetbrains.kotlin.konan.test.blackbox.support.util.ThreadSafeCache
import org.jetbrains.kotlin.konan.test.blackbox.support.util.flatMapToSet
import org.jetbrains.kotlin.konan.test.blackbox.support.util.getAbsoluteFile
import org.jetbrains.kotlin.konan.test.blackbox.support.util.mapToSet
import org.jetbrains.kotlin.swiftexport.standalone.*
import org.jetbrains.kotlin.test.backend.handlers.UpdateTestDataSupport
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertTrue
import org.jetbrains.kotlin.utils.KotlinNativePaths
import org.jetbrains.kotlin.utils.filterToSetOrEmpty
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.extension.ExtendWith
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.div
import org.jetbrains.kotlin.swiftexport.standalone.UnsupportedDeclarationReporterKind
import org.jetbrains.kotlin.swiftexport.standalone.config.SwiftExportConfig
import org.jetbrains.kotlin.swiftexport.standalone.config.SwiftModuleConfig

@ExtendWith(SwiftExportTestSupport::class, UpdateTestDataSupport::class)
abstract class AbstractSwiftExportTest {
    lateinit var testRunSettings: TestRunSettings
    lateinit var testRunProvider: TestRunProvider

    private val binariesDir get() = testRunSettings.get<Binaries>().testBinariesDir
    protected fun buildDir(testName: String) = binariesDir.resolve(testName)
    protected val targets: KotlinNativeTargets get() = testRunSettings.get()
    protected val testCompilationFactory = TestCompilationFactory()
    private val compiledSwiftCache = ThreadSafeCache<SwiftExportModule, TestCompilationArtifact.Swift.Module>()
    private val tmpdir = FileUtil.createTempDirectory("SwiftExportIntegrationTests", null, false)

    protected abstract fun runCompiledTest(
        testPathFull: File,
        testCase: TestCase,
        swiftExportOutputs: Set<SwiftExportModule>,
        swiftModules: Set<TestCompilationArtifact.Swift.Module>,
        kotlinBinaryLibrary: TestCompilationArtifact.BinaryLibrary,
    )

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

        val config = SwiftExportConfig(
            outputPath = tmpdir.toPath().resolve(testDir),
            stableDeclarationsOrder = true,
            distribution = Distribution(KonanHome.konanHomePath),
            konanTarget = targets.testTarget,
            errorTypeStrategy = ErrorTypeStrategy.Fail,
            unsupportedTypeStrategy = ErrorTypeStrategy.SpecialType
        )

        val input = modulesToExport.mapToSet { testModule ->
            testModule.constructSwiftInput(
                originalTestCase.freeCompilerArgs,
                SwiftModuleConfig(
                    rootPackage = testModule.swiftExportConfigMap()?.get(SwiftModuleConfig.ROOT_PACKAGE),
                    unsupportedDeclarationReporterKind = getUnsupportedDeclarationsReporterKind(testModule.swiftExportConfigMap())
                )
            )
        }

        // run swift export
        val swiftExportOutputs: Set<SwiftExportModule> = runSwiftExport(input, config).getOrThrow()

        // compile kotlin into binary
        val additionalKtFiles: Set<Path> = mutableSetOf<Path>()
            .apply { swiftExportOutputs.collectKotlinBridgeFilesRecursively(into = this) }

        val kotlinFiles = modulesToExport.flatMapToSet { module -> module.files.map { file -> file.location } }
        val kotlinBinaryLibraryName = testPathFull.name + "Kotlin"

        val resultingTestCase = generateSwiftExportTestCase(
            testPathFull,
            kotlinBinaryLibraryName,
            kotlinFiles.toList() + additionalKtFiles.map { it.toFile() },
            modules = modulesToExport
                .flatMapToSet {
                    it.allRegularDependencies.filterIsInstance<TestModule.Exclusive>().toSet()
                } - modulesToExport,
        )

        // TODO: we don't need to compile Kotlin binary for generation tests.
        val kotlinBinaryLibrary = testCompilationFactory.testCaseToBinaryLibrary(
            resultingTestCase, testRunSettings,
            kind = BinaryLibraryKind.STATIC,
        ).result.assertSuccess().resultingArtifact

        // compile swift into binary
        val swiftModules = swiftExportOutputs.flatMapToSet {
            it.compile(
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
        moduleConfig: SwiftModuleConfig,
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
            config = moduleConfig
        )
    }

    private fun Collection<SwiftExportModule>.collectKotlinBridgeFilesRecursively(into: MutableSet<Path>) =
        forEach { module -> module.collectKotlinBridgeFilesRecursively(into) }

    private fun SwiftExportModule.collectKotlinBridgeFilesRecursively(into: MutableSet<Path>) {
        if (this is SwiftExportModule.BridgesToKotlin) into.add(files.kotlinBridges)
        dependencies.filterIsInstance<SwiftExportModule.BridgesToKotlin>().collectKotlinBridgeFilesRecursively(into)
    }

    protected fun SwiftExportModule.compile(
        testPathFull: File,
        allModules: Set<SwiftExportModule>,
    ): Set<TestCompilationArtifact.Swift.Module> {
        val deps = resolvedDependencies(allModules).flatMapToSet { it.compile(testPathFull, allModules) }
        val compiledSwiftModule = when (this) {
            is SwiftExportModule.BridgesToKotlin -> compile(testPathFull, allModules)
            is SwiftExportModule.SwiftOnly -> compile(testPathFull)
        }
        return deps + compiledSwiftModule
    }

    private fun SwiftExportModule.SwiftOnly.compile(
        testPathFull: File,
    ): Set<TestCompilationArtifact.Swift.Module> {
        val compiledSwiftModule = compiledSwiftCache.computeIfAbsent(this) {
            val swiftModuleDir = buildDir(testPathFull.name).resolve("SwiftModules").also { it.mkdirs() }
            return@computeIfAbsent compileSwiftModule(
                swiftModuleDir = swiftModuleDir,
                swiftModuleName = name,
                sources = listOf(swiftApi.toFile()),
                kotlinBridgeModuleMap = null,
                deps = emptyList(),
            )
        }
        return setOf(compiledSwiftModule)
    }

    private fun SwiftExportModule.BridgesToKotlin.compile(
        testPathFull: File,
        allModules: Set<SwiftExportModule>,
    ): Set<TestCompilationArtifact.Swift.Module> {
        val deps = resolvedDependencies(allModules).flatMapToSet { it.compile(testPathFull, allModules) }
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
        deps: Collection<TestCompilationArtifact.Swift.Module>,
    ): TestCompilationArtifact.Swift.Module {
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
                *deps.flatMap { dependency ->
                    listOf(
                        "-L", dependency.binaryLibrary.parentFile.absolutePath,
                        "-I", dependency.binaryLibrary.parentFile.absolutePath,
                        "-l${dependency.moduleName}",
                    )
                }.toTypedArray(),
                "-emit-module", "-parse-as-library", "-emit-library", "-static", "-enable-library-evolution",
                "-module-name", swiftModuleName,
                "-package-name", "SwiftExportTests",
            ),
            outputFile = { it.binaryLibrary },
        ).result.assertSuccess().resultingArtifact
    }

    protected fun generateSwiftExportTestCase(
        testPathFull: File,
        testName: String = testPathFull.name,
        sources: List<File>,
        modules: Set<TestModule.Exclusive> = emptySet(),
        dependencies: Set<TestModule.Given>? = null,
    ): TestCase {
        val module = TestModule.newDefaultModule()
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
            modules = setOf(module) + modules,
            freeCompilerArgs = TestCompilerArgs(
                listOf(
                    "-opt-in",
                    "kotlin.experimental.ExperimentalNativeApi",
                    "-opt-in",
                    "kotlinx.cinterop.ExperimentalForeignApi",
                    "-opt-in",
                    "kotlin.native.internal.InternalForKotlinNative", // for uninitialized object instance manipulation, and ExternalRCRef.
                    "-Xbinary=swiftExport=true",
                    "-Xcontext-parameters",
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
            initialize(dependencies, null)
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

private fun getUnsupportedDeclarationsReporterKind(configMap: Map<String, String>?): UnsupportedDeclarationReporterKind {
    return configMap?.get(SwiftModuleConfig.UNSUPPORTED_DECLARATIONS_REPORTER_KIND)
        ?.let { value ->
            UnsupportedDeclarationReporterKind.entries
                .singleOrNull { it.name.equals(value, ignoreCase = true) }
        } ?: UnsupportedDeclarationReporterKind.Silent
}

object KonanHome {
    private const val KONAN_HOME_PROPERTY_KEY = "kotlin.internal.native.test.nativeHome"

    val konanHomePath: String
        get() = System.getProperty(KONAN_HOME_PROPERTY_KEY)
            ?: error("Missing System property: '$KONAN_HOME_PROPERTY_KEY'")
}
