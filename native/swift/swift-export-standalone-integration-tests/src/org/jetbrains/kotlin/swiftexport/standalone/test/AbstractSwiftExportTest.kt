/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone.test

import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.konan.test.blackbox.support.*
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.SwiftCompilation
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationFactory
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationFactory.ProduceStaticCache
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.group.isIgnoredTarget
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunCheck
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunChecks
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunProvider
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.*
import org.jetbrains.kotlin.konan.test.blackbox.support.util.ExternalSourceTransformers
import org.jetbrains.kotlin.konan.test.blackbox.support.util.ThreadSafeCache
import org.jetbrains.kotlin.konan.test.blackbox.support.util.flatMapToSet
import org.jetbrains.kotlin.konan.test.blackbox.support.util.getAbsoluteFile
import org.jetbrains.kotlin.konan.test.blackbox.support.util.mapToSet
import org.jetbrains.kotlin.swiftexport.standalone.*
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertTrue
import org.jetbrains.kotlin.utils.KotlinNativePaths
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.div
import org.jetbrains.kotlin.swiftexport.standalone.UnsupportedDeclarationReporterKind
import org.jetbrains.kotlin.swiftexport.standalone.config.SwiftExportConfig
import org.jetbrains.kotlin.swiftexport.standalone.config.SwiftModuleConfig
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(SwiftExportTestSupport::class)
abstract class AbstractSwiftExportTest : ExternalSourceTransformersProvider {
    lateinit var testRunSettings: TestRunSettings
    lateinit var testRunProvider: TestRunProvider

    /*
    * There are cases in Swift Export usages when we want to test the translation against some prebuilt KLIB. As an example, we want
    * to verify the translation of user code that uses kotlinx.coroutines.
    *
    * For such cases it is possible to pass a klib into the translation unconditionally. KLIBs that are found here would be passed into both
    * execution and generation context, simulating a case when a user has a dependency in their Gradle project.
    * */
    var givenModules: Set<TestModule.Given> = emptySet()

    private val binariesDir get() = testRunSettings.get<Binaries>().testBinariesDir
    protected fun buildDir(testName: String) = binariesDir.resolve(testName)
    protected val targets: KotlinNativeTargets get() = testRunSettings.get()
    protected val testCompilationFactory = TestCompilationFactory()
    private val compiledSwiftCache = ThreadSafeCache<SwiftExportModule, TestCompilationArtifact.Swift.Module>()

    protected fun isTestIgnored(testDir: String): Boolean {
        val testPathFull = getAbsoluteFile(testDir)
        val testFile = (testPathFull.toPath() / "${testPathFull.name}.kt").toFile()
        return testRunSettings.isIgnoredTarget(testFile)
    }

    protected fun runConvertToSwift(@TestDataFile testDir: String): Pair<Set<SwiftExportModule>, TestCase> {
        val testPathFull = getAbsoluteFile(testDir)
        val testFile = (testPathFull.toPath() / "${testPathFull.name}.kt").toFile()

        val testCaseId = TestCaseId.TestDataFile(testFile)
        val originalTestCase = testRunProvider.testCaseGroupProvider
            .getTestCaseGroup(testCaseId.testCaseGroupId, testRunSettings)
            ?.getByName(testCaseId)!!
            .copyAndAddModules(givenModules)

        val modulesToExport = (originalTestCase.rootModules + originalTestCase.modules + givenModules).mapToSet {
            createInputModule(
                testModule = it,
                originalTestCase = originalTestCase,
                shouldBeFullyExported = it.shouldBeExportedToSwift() || originalTestCase.rootModules.contains(it)
            )
        }

        val config = SwiftExportConfig(
            outputPath = buildDir(testPathFull.name).resolve(testDir).toPath(),
            stableDeclarationsOrder = true,
            distribution = Distribution(KonanHome.konanHomePath),
            konanTarget = targets.testTarget,
            errorTypeStrategy = ErrorTypeStrategy.Fail,
            unsupportedTypeStrategy = ErrorTypeStrategy.SpecialType,
        )

        // run swift export
        val swiftExportOutputs = runSwiftExport(
            modulesToExport,
            config
        ).getOrThrow()

        // compile kotlin into binary
        val additionalKtFiles: Set<Path> = mutableSetOf<Path>()
            .apply { swiftExportOutputs.collectKotlinBridgeFilesRecursively(into = this) }

        val kotlinFiles = originalTestCase.rootModules.flatMapToSet { module -> module.files.map { file -> file.location } }
        val kotlinBinaryLibraryName = testPathFull.name + "Kotlin"

        val resultingTestCase = generateSwiftExportTestCase(
            testPathFull,
            kotlinBinaryLibraryName,
            kotlinFiles.toList() + additionalKtFiles.map { it.toFile() },
            modules = originalTestCase.rootModules
                .flatMapToSet {
                    it.allRegularDependencies.filterIsInstance<TestModule.Exclusive>().toSet()
                } - originalTestCase.rootModules,
            dependencies = givenModules
        )
        return swiftExportOutputs to resultingTestCase
    }

    private fun createInputModule(
        testModule: TestModule,
        originalTestCase: TestCase,
        shouldBeFullyExported: Boolean
    ): InputModule {
        val config = (testModule as? TestModule.Exclusive)?.swiftExportConfigMap()
        return testModule.constructSwiftInput(
            originalTestCase.freeCompilerArgs,
            SwiftModuleConfig(
                rootPackage = config?.get(SwiftModuleConfig.ROOT_PACKAGE),
                unsupportedDeclarationReporterKind = getUnsupportedDeclarationsReporterKind(config),
                shouldBeFullyExported = shouldBeFullyExported,
            )
        )
    }

    private fun TestModule.constructSwiftInput(
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
            name = if (moduleToTranslate.name == "kotlinx-coroutines-core.klib") "KotlinxCoroutinesCore" else moduleToTranslate.name,
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

    override fun getSourceTransformers(testDataFile: File): ExternalSourceTransformers? = null
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

private fun TestCase.copyAndAddModules(givenModules: Set<TestModule.Given>?): TestCase = TestCase(
    id = id, kind = kind, modules = modules, freeCompilerArgs = freeCompilerArgs, nominalPackageName = nominalPackageName, checks = checks,
    extras = extras, fileCheckStage = fileCheckStage, expectedFailure = expectedFailure
).apply { initialize(givenModules, null) }
