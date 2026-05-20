/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.blackbox

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.CommonJsAndWasmCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.KotlinWasmCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.cliArgument
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.js.test.klib.CustomWebCompilerSettings
import org.jetbrains.kotlin.js.test.klib.collectDependencies
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.platform.wasm.isWasmWasi
import org.jetbrains.kotlin.test.GroupingStageInputArtifact
import org.jetbrains.kotlin.test.backend.codegenSuppressionChecker
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives.USE_NEW_EXCEPTION_HANDLING_PROPOSAL
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives.USE_OLD_EXCEPTION_HANDLING_PROPOSAL
import org.jetbrains.kotlin.test.frontend.fir.getTransitivesAndFriends
import org.jetbrains.kotlin.test.groupingStageInputs
import org.jetbrains.kotlin.test.klib.CustomKlibCompilerException
import org.jetbrains.kotlin.test.klib.CustomKlibCompilerSecondStageFacade
import org.jetbrains.kotlin.test.model.AbstractGroupingStageTestFacade
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestArtifactKind
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.model.WasmFolderBinaryArtifact
import org.jetbrains.kotlin.test.services.sourceFileProvider
import org.jetbrains.kotlin.test.services.sourceProviders.MainFunctionForBlackBoxTestsSourceProvider
import org.jetbrains.kotlin.test.services.BatchingPackageInserter
import org.jetbrains.kotlin.test.services.CompilationStage
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.artifactsProvider
import org.jetbrains.kotlin.test.services.configuration.WasmEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.WasmEnvironmentConfigurator.Companion.WASM_BASE_FILE_NAME
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.services.targetPlatform
import org.jetbrains.kotlin.test.services.temporaryDirectoryManager
import org.jetbrains.kotlin.test.services.testInfo
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.mapToSetOrEmpty
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

/**
 * An implementation of [CustomKlibCompilerSecondStageFacade] for WasmJs.
 * Suits any version backend version: either current, or old released. It's specified by `webCompilerSettings` param.
 */
class WasmJsCompilerSecondStageFacade private constructor(
    val testServices: TestServices,
    private val customWebCompilerSettings: CustomWebCompilerSettings
) {
    class Grouping(
        val testServices: TestServices,
        private val customWebCompilerSettings: CustomWebCompilerSettings
    ) : AbstractGroupingStageTestFacade<GroupingStageInputArtifact, BinaryArtifacts.Wasm>() {
        override fun transform(inputArtifact: GroupingStageInputArtifact): BinaryArtifacts.Wasm {
            val servicesOfSomeModule = inputArtifact.nonGroupingStageOutputs.first().testServices
            val someModule = servicesOfSomeModule.moduleStructure.modules.last()
            var someLibrary: File? = null

            val facade = WasmJsCompilerSecondStageFacade(testServices, customWebCompilerSettings)

            val tempDir = testServices.temporaryDirectoryManager.getOrCreateTempDirectory("combined-sources")
            val filteredOutputs = mutableListOf<Pair<TestServices, BinaryArtifacts.KLib>>()
            for (output in inputArtifact.nonGroupingStageOutputs) {
                val services = output.testServices
                val module = services.moduleStructure.modules.last()
                if (!services.codegenSuppressionChecker.failuresInModuleAreIgnored(module)) {
                    val artifact = services.artifactsProvider.getArtifact(module, ArtifactKinds.KLib)
                    filteredOutputs.add(services to artifact)
                }
            }

            val allFilesMap = mutableMapOf<String, TestFile>()
            for ((services, _) in filteredOutputs) {
                val module = services.moduleStructure.modules.last()
                for (file in module.files) {
                    if (file.name == "ProxyBatchLauncher.kt" || file.name.startsWith("__launcher_")) continue

                    val content = services.sourceFileProvider.getContentOfSourceFile(file)
                    val uniqueName = if (file.isAdditional) {
                        file.name
                    } else {
                        "${module.name.replace('.', '_').replace(' ', '_')}_${file.name}"
                    }

                    if (allFilesMap.containsKey(uniqueName)) continue

                    val tempFile = tempDir.resolve(uniqueName)
                    tempFile.writeText(content)
                    allFilesMap[uniqueName] = TestFile(
                        file.relativePath,
                        content,
                        tempFile,
                        file.startLineNumberInOriginalFile,
                        file.isAdditional,
                        file.directives
                    )
                }
            }

            val proxyLauncherContent = buildString {
                append("package proxy.batch.launcher\n\n")
                append("import kotlin.test.Test\n")
                append("import kotlin.test.assertEquals\n\n")
                for ([services, _] in filteredOutputs) {
                    val module = services.moduleStructure.modules.last()
                    val additionalPackage = BatchingPackageInserter.computePackage(services.testInfo)
                    val fileWithBox = module.files.firstOrNull {
                        val content = services.sourceFileProvider.getContentOfSourceFile(it)
                        MainFunctionForBlackBoxTestsSourceProvider.containsBoxMethod(content)
                    }
                    val originalPackage = fileWithBox?.let { MainFunctionForBlackBoxTestsSourceProvider.detectPackage(it) }
                    val boxFqName = if (originalPackage != null) "$additionalPackage.$originalPackage.box" else "$additionalPackage.box"

                    val uniqueClassName = "ProxyLauncher_${module.name.replace('.', '_').replace(' ', '_')}"
                    append("class $uniqueClassName {\n")
                    append("    @Test\n")
                    append("    fun runTest() {\n")
                    append("        val result = $boxFqName()\n")
                    append("        assertEquals(\"OK\", result, \"Test failed with: \$result\")\n")
                    append("    }\n")
                    append("}\n\n")
                }

                append("\n@kotlin.wasm.WasmExport\n")
                append("fun hasTestFailures(): Boolean {\n")
                append("    return kotlin.test.hasTestFailures()\n")
                append("}\n")
            }
            val tempFile = tempDir.resolve("ProxyBatchLauncher.kt")
            tempFile.writeText(proxyLauncherContent)

            val batchLauncherFile = TestFile(
                "ProxyBatchLauncher.kt",
                proxyLauncherContent,
                tempFile,
                0,
                true,
                someModule.files.first().directives
            )

            val combinedModule = someModule.copy(files = allFilesMap.values.toList() + batchLauncherFile)

            // Step 1: Compile combined sources into a single KLIB
            val batchKlibFile = tempDir.resolve("batch.klib")
            val regularDependencies = mutableSetOf<String>()
            val friendDependencies = mutableSetOf<String>()
            for ((services, _) in filteredOutputs) {
                val mainModule = services.moduleStructure.modules.last()
                mainModule.collectDependencies(services, customWebCompilerSettings).let { (regular, friend) ->
                    regularDependencies += regular
                    friendDependencies += friend
                }
            }

            val maxLanguageVersion = filteredOutputs.maxOf { (services, _) ->
                services.moduleStructure.modules.last().languageVersionSettings.languageVersion
            }

            val allLanguageFeatures = filteredOutputs.flatMap { (services, _) ->
                services.moduleStructure.modules.last().directives[LanguageSettingsDirectives.LANGUAGE]
            }.distinct()

            facade.compileSourcesToKlib(
                combinedModule,
                combinedModule.files.map { it.originalFile },
                batchKlibFile,
                languageVersion = maxLanguageVersion.versionString,
                customLanguageFeatures = allLanguageFeatures,
                regularDependencies,
                friendDependencies
            )

            // Step 2: Compile KLIB into WASM executable
            val moduleNameHash = someModule.name.hashCode().toHexString()
            val (exitCode, output, executableFolder) = facade.runCli(
                someModule.copy(files = emptyList()),
                moduleNameHash,
                customLanguageFeatures = allLanguageFeatures,
                mainLibraries = listOf(batchKlibFile.absolutePath),
                regularDependencies = regularDependencies,
                friendDependencies = friendDependencies,
            )
            if (exitCode == ExitCode.OK) {
                // Successfully compiled. Return the artifact.
                // TODO consider creating WasmCompilationSetsBinaryArtifact instead, holding additional artifacts for DCE and optimised compilations
                return WasmFolderBinaryArtifact(executableFolder)
            } else {
                // Throw an exception to abort further test execution.
                throw CustomKlibCompilerException(exitCode, output.toString(Charsets.UTF_8.name()))
            }
        }

        override val inputKind: TestArtifactKind<GroupingStageInputArtifact>
            get() = GroupingStageInputArtifact.Kind
        override val outputKind: TestArtifactKind<BinaryArtifacts.Wasm>
            get() = ArtifactKinds.Wasm
    }

    class NonGrouping(
        testServices: TestServices,
        private val customWebCompilerSettings: CustomWebCompilerSettings,
    ) : CustomKlibCompilerSecondStageFacade<BinaryArtifacts.Wasm>(testServices) {
        override val outputKind get() = ArtifactKinds.Wasm

        override fun isMainModule(module: TestModule) = module == WasmEnvironmentConfigurator.getMainModule(testServices)
        override fun collectDependencies(module: TestModule) = module.collectDependencies(testServices, CompilationStage.SECOND)

        override fun compileBinary(
            module: TestModule,
            customArgs: List<String>,
            mainLibrary: String,
            regularDependencies: Set<String>,
            friendDependencies: Set<String>,
        ): BinaryArtifacts.Wasm {
            val facade = WasmJsCompilerSecondStageFacade(testServices, customWebCompilerSettings)
            val (exitCode, output, executableFolder) = facade.runCli(
                module,
                module.name,
                customLanguageFeatures = module.directives[LanguageSettingsDirectives.LANGUAGE],
                mainLibraries = listOf(mainLibrary),
                regularDependencies = regularDependencies,
                friendDependencies = friendDependencies,
            )

            if (exitCode == ExitCode.OK) {
                // Successfully compiled. Return the artifact.
                return WasmFolderBinaryArtifact(executableFolder)
            } else {
                // Throw an exception to abort further test execution.
                throw CustomKlibCompilerException(exitCode, output.toString(Charsets.UTF_8.name()))
            }
        }
    }

    data class CliRunResult(val exitCode: ExitCode, val output: ByteArrayOutputStream, val executableFolder: File)

    private fun compileSourcesToKlib(
        module: TestModule,
        sources: List<File>,
        klibOutputFile: File,
        languageVersion: String,
        customLanguageFeatures: List<String>,
        regularDependencies: Set<String>,
        friendDependencies: Set<String>,
    ): ExitCode {
        val compilerXmlOutput = ByteArrayOutputStream()
        val exitCode = PrintStream(compilerXmlOutput).use { printStream ->
            val isWasmWasi = module.targetPlatform(testServices).isWasmWasi()
            val regularAndFriendDependencies = regularDependencies + friendDependencies
            customWebCompilerSettings.customKlibCompiler.callCompiler(
                output = printStream,
                listOfNotNull(
                    runIf(isWasmWasi) {
                        KotlinWasmCompilerArguments::wasmTarget.cliArgument(WasmTarget.WASI.alias)
                    },
                    CommonCompilerArguments::languageVersion.cliArgument(languageVersion),
                    CommonJsAndWasmCompilerArguments::outputDir.cliArgument, klibOutputFile.parentFile.path,
                    CommonJsAndWasmCompilerArguments::moduleName.cliArgument, klibOutputFile.nameWithoutExtension,
                    KotlinWasmCompilerArguments::wasmEnableArrayRangeChecks.cliArgument,
                    CommonCompilerArguments::disableDefaultScriptingPlugin.cliArgument,
                    runIf(LanguageSettingsDirectives.ALLOW_KOTLIN_PACKAGE in module.directives) {
                        CommonCompilerArguments::allowKotlinPackage.cliArgument
                    }
                ),
                sources.map { it.absolutePath },
                runIf(regularAndFriendDependencies.isNotEmpty()) {
                    listOf(
                        CommonJsAndWasmCompilerArguments::libraries.cliArgument,
                        regularAndFriendDependencies.joinToString(File.pathSeparator),
                    )
                },
                runIf(friendDependencies.isNotEmpty()) {
                    listOf(CommonJsAndWasmCompilerArguments::friendModules.cliArgument(friendDependencies.joinToString(File.pathSeparator)))
                },
                customLanguageFeatures
                    .filterNot { LanguageFeature.valueOf(it.removePrefix("+").removePrefix("-")).testOnly }
                    .map { CommonCompilerArguments::manuallyConfiguredFeatures.cliArgument + ":$it" },
            )
        }
        if (exitCode != ExitCode.OK) {
            val outputStr = compilerXmlOutput.toString(Charsets.UTF_8.name())
            throw CustomKlibCompilerException(exitCode, outputStr)
        }
        return exitCode
    }

    fun runCli(
        module: TestModule,
        dirName: String,
        customLanguageFeatures: List<String>,
        mainLibraries: List<String>,
        regularDependencies: Set<String>,
        friendDependencies: Set<String>,
    ): CliRunResult {
        val wasmArtifactFile = testServices.temporaryDirectoryManager.getOrCreateTempDirectory(dirName).resolve("$WASM_BASE_FILE_NAME.wasm")
        val compilerXmlOutput = ByteArrayOutputStream()
        val isWasmWasi = module.targetPlatform(testServices).isWasmWasi()

        val groupingStageInputs = testServices.groupingStageInputs
        val allDirectivesOfFirstModule = groupingStageInputs.first().testServices.moduleStructure.allDirectives
        val isNewEH = USE_NEW_EXCEPTION_HANDLING_PROPOSAL in allDirectivesOfFirstModule
        val isOldEH = USE_OLD_EXCEPTION_HANDLING_PROPOSAL in allDirectivesOfFirstModule
        for (input in groupingStageInputs) {
            if (isNewEH) require(USE_NEW_EXCEPTION_HANDLING_PROPOSAL in input.testServices.moduleStructure.allDirectives) {
                "Malformed group: all tests in group must have same USE_NEW_EXCEPTION_HANDLING_PROPOSAL setting"
            }
            if (isOldEH) require(USE_OLD_EXCEPTION_HANDLING_PROPOSAL in input.testServices.moduleStructure.allDirectives) {
                "Malformed group: all tests in group must have same USE_OLD_EXCEPTION_HANDLING_PROPOSAL setting"
            }
        }

        val exitCode = PrintStream(compilerXmlOutput).use { printStream ->
            val regularAndFriendDependencies = regularDependencies + friendDependencies + mainLibraries.drop(1)
            customWebCompilerSettings.customKlibCompiler.callCompiler(
                output = printStream,
                listOfNotNull(
                    runIf(isWasmWasi) {
                        KotlinWasmCompilerArguments::wasmTarget.cliArgument(WasmTarget.WASI.alias)
                    },
                    CommonJsAndWasmCompilerArguments::irProduceJs.cliArgument,
                    runIf(mainLibraries.isNotEmpty()) {
                        KotlinWasmCompilerArguments::includes.cliArgument(mainLibraries.first())
                    },
                    CommonJsAndWasmCompilerArguments::outputDir.cliArgument, wasmArtifactFile.parentFile.path,
                    CommonJsAndWasmCompilerArguments::moduleName.cliArgument, WASM_BASE_FILE_NAME,
                    KotlinWasmCompilerArguments::wasmEnableArrayRangeChecks.cliArgument,
                    CommonCompilerArguments::disableDefaultScriptingPlugin.cliArgument,
                ),
                module.files.map { it.originalFile.absolutePath },
                runIf(regularAndFriendDependencies.isNotEmpty()) {
                    listOf(
                        CommonJsAndWasmCompilerArguments::libraries.cliArgument,
                        regularAndFriendDependencies.joinToString(File.pathSeparator),
                    )
                },
                runIf(friendDependencies.isNotEmpty()) {
                    listOf(CommonJsAndWasmCompilerArguments::friendModules.cliArgument(friendDependencies.joinToString(File.pathSeparator)))
                },
                runIf(isNewEH) {
                    listOf(KotlinWasmCompilerArguments::wasmUseNewExceptionProposal.cliArgument)
                },
                runIf(isOldEH) {
                    listOf(KotlinWasmCompilerArguments::wasmUseNewExceptionProposal.cliArgument("false"))
                },
                customLanguageFeatures
                    .filterNot { LanguageFeature.valueOf(it.removePrefix("+").removePrefix("-")).testOnly }
                    .map { CommonCompilerArguments::manuallyConfiguredFeatures.cliArgument + ":$it" },
            )
        }

        if (exitCode == ExitCode.OK) {
            // Successfully compiled. Return the artifact.
            require(wasmArtifactFile.exists()) {
                "Internal testinfra error: Couldn't find expected generated wasm artifact ${wasmArtifactFile.absolutePath}"
            }

            return CliRunResult(exitCode, compilerXmlOutput, wasmArtifactFile.parentFile)
        } else {
            // Throw an exception to abort further test execution.
            throw CustomKlibCompilerException(exitCode, compilerXmlOutput.toString(Charsets.UTF_8.name()))
        }
    }
}

fun TestModule.collectDependencies(
    testServices: TestServices,
    customWebCompilerSettings: CustomWebCompilerSettings,
): Pair<Set<String>, Set<String>> {
    val (transitiveLibraries: List<File>, friendLibraries: List<File>) = getTransitivesAndFriends(module = this, testServices)

    val regularDependencies: Set<String> = buildSet {
        add(customWebCompilerSettings.stdlib.absolutePath)
        add(customWebCompilerSettings.kotlinTest.absolutePath)
        transitiveLibraries.mapTo(this) { it.absolutePath }
    }

    val friendDependencies: Set<String> = friendLibraries.mapToSetOrEmpty { it.absolutePath }

    return regularDependencies to friendDependencies
}
