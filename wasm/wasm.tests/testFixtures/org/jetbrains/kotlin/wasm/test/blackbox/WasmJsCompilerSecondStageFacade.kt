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
import org.jetbrains.kotlin.config.ReturnValueCheckerMode
import org.jetbrains.kotlin.js.test.klib.CustomWebCompilerSettings
import org.jetbrains.kotlin.js.test.klib.collectDependencies
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.platform.wasm.isWasmWasi
import org.jetbrains.kotlin.test.GroupingStageInputArtifact
import org.jetbrains.kotlin.test.backend.codegenSuppressionChecker
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.RETURN_VALUE_CHECKER_MODE
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
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
import org.jetbrains.kotlin.test.impl.shouldIsolateTestInGroupingConfiguration
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
    companion object {
        init {
            try {
                System.setProperty("kotlinc.test.allow.testonly.language.features", "true")
            } catch (e: Exception) {
                // In some environments setting properties might be prohibited
            }
        }
    }

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
            val filteredOutputs = mutableListOf<Triple<TestServices, TestModule, BinaryArtifacts.KLib>>()
            for (output in inputArtifact.nonGroupingStageOutputs) {
                val services = output.testServices
                for (module in services.moduleStructure.modules) {
                    if (!services.codegenSuppressionChecker.failuresInModuleAreIgnored(module)) {
                        val artifact = try {
                            services.artifactsProvider.getArtifact(module, ArtifactKinds.KLib)
                        } catch (e: Exception) {
                            continue
                        }
                        filteredOutputs.add(Triple(services, module, artifact))
                    }
                }
            }

            // Check if this is a single isolated test (which might be multi-module)
            val isIsolatedBatch = inputArtifact.nonGroupingStageOutputs.map { it.testServices.testInfo }.distinct().size == 1 &&
                    inputArtifact.nonGroupingStageOutputs.first().testServices.shouldIsolateTestInGroupingConfiguration(fileGenerationPhase = true)

            if (isIsolatedBatch) {
                val services = inputArtifact.nonGroupingStageOutputs.first().testServices
                val additionalPackage = BatchingPackageInserter.computePackage(services.testInfo)
                val testModules = filteredOutputs.map { it.second }
                val mainModule = testModules.last()
                val mainArtifact = filteredOutputs.last().third

                val contentForTriggers = testModules.joinToString("\n") { m -> m.files.joinToString("\n") { f -> f.originalContent } }
                val hasReflectionTriggers = contentForTriggers.contains("::class.qualifiedName") ||
                        contentForTriggers.contains("::class.simpleName") ||
                        contentForTriggers.contains("::class.toString()") ||
                        contentForTriggers.contains("typeOf<") ||
                        contentForTriggers.contains("import kotlin.reflect.") ||
                        testModules.any { m ->
                            JvmEnvironmentConfigurationDirectives.WITH_REFLECT in m.directives ||
                                    m.files.any { f -> JvmEnvironmentConfigurationDirectives.WITH_REFLECT in f.directives }
                        }

                val isPatched = !hasReflectionTriggers

                var fileWithBox: TestFile? = null
                for (module in testModules) {
                    fileWithBox = module.files.firstOrNull {
                        val content = services.sourceFileProvider.getContentOfSourceFile(it)
                        MainFunctionForBlackBoxTestsSourceProvider.containsBoxMethod(content)
                    }
                    if (fileWithBox != null) break
                }

                val batchLauncherFile = if (fileWithBox != null) {
                    val originalPackage = MainFunctionForBlackBoxTestsSourceProvider.detectPackage(fileWithBox)
                    val boxFqName = if (isPatched) {
                        if (originalPackage != null) "$additionalPackage.$originalPackage.box" else "$additionalPackage.box"
                    } else {
                        if (originalPackage != null) "$originalPackage.box" else "box"
                    }
                    val uniqueClassName = "ProxyLauncher_${additionalPackage.hashCode().toUInt().toString(36)}"

                    val proxyLauncherContent = """
                        import kotlin.test.Test
                        import kotlin.test.assertEquals

                        class $uniqueClassName {
                            @Test
                            fun runTest() {
                                val result = $boxFqName()
                                assertEquals("OK", result, "Test failed with: ${'$'}result")
                            }
                        }

                        @kotlin.wasm.WasmExport
                        fun hasTestFailures(): Boolean {
                            return kotlin.test.hasTestFailures()
                        }
                    """.trimIndent()

                    val tempFile = tempDir.resolve("ProxyBatchLauncher.kt")
                    tempFile.writeText(proxyLauncherContent)
                    TestFile("ProxyBatchLauncher.kt", proxyLauncherContent, tempFile, 0, true, mainModule.files.first().directives)
                } else null

                val (regularDependencies, friendDependencies) = mainModule.collectDependencies(services, customWebCompilerSettings)
                val (exitCode, output, executableFolder) = facade.runCli(
                    mainModule.copy(files = listOfNotNull(batchLauncherFile)),
                    mainModule.name.hashCode().toHexString(),
                    customLanguageFeatures = mainModule.directives[LanguageSettingsDirectives.LANGUAGE],
                    customOptIns = mainModule.directives[LanguageSettingsDirectives.OPT_IN],
                    allowKotlinPackage = LanguageSettingsDirectives.ALLOW_KOTLIN_PACKAGE in mainModule.directives,
                    mainLibraries = filteredOutputs.map { it.third.outputFile.absolutePath }.reversed(),
                    regularDependencies = regularDependencies,
                    friendDependencies = friendDependencies,
                )
                if (exitCode == ExitCode.OK) {
                    // Copy all additional files to the executable folder
                    for (testModule in testModules) {
                        for (file in testModule.files) {
                            if (file.name.endsWith(".mjs") || file.name.endsWith(".js")) {
                                val content = services.sourceFileProvider.getContentOfSourceFile(file)
                                executableFolder.resolve(file.name).writeText(content)
                            }
                        }
                    }
                    return WasmFolderBinaryArtifact(executableFolder)
                } else {
                    throw CustomKlibCompilerException(exitCode, output.toString(Charsets.UTF_8.name()))
                }
            }

            val allFilesMap = mutableMapOf<String, TestFile>()
            for ((services, module, artifact) in filteredOutputs) {
                for (file in module.files) {
                    if (file.name == "ProxyBatchLauncher.kt" || file.name.startsWith("__launcher_")) continue

                    val content = services.sourceFileProvider.getContentOfSourceFile(file)
                    val additionalPackage = BatchingPackageInserter.computePackage(services.testInfo)
                    val packageHash = additionalPackage.hashCode().toUInt().toString(36)
                    val moduleHash = module.name.hashCode().toUInt().toString(36)
                    val uniqueName = "${file.name.substringBeforeLast(".")}_${packageHash}_${moduleHash}.kt"

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
                append("import kotlin.test.Test\n")
                append("import kotlin.test.assertEquals\n\n")
                for ((services, triples) in filteredOutputs.groupBy { it.first }) {
                    val mainModule = services.moduleStructure.modules.last()
                    val additionalPackage = BatchingPackageInserter.computePackage(services.testInfo)
                    val fileWithBox = mainModule.files.firstOrNull {
                        val content = services.sourceFileProvider.getContentOfSourceFile(it)
                        MainFunctionForBlackBoxTestsSourceProvider.containsBoxMethod(content)
                    }
                    if (fileWithBox == null) continue

                    val originalPackage = fileWithBox.let { MainFunctionForBlackBoxTestsSourceProvider.detectPackage(it) }

                    // Since we are in the batching path, this test is NOT isolated (or at least it's being grouped)
                    // But wait, if it's isolated but we didn't hit the early return?
                    // Actually, only non-isolated tests should reach here if we want them patched.
                    // But wait, what if an isolated test DOES NOT have a box() method? (Unlikely for box tests)
                    
                    val boxFqName = if (originalPackage != null) "$additionalPackage.$originalPackage.box" else "$additionalPackage.box"

                    val uniqueClassName = "ProxyLauncher_${additionalPackage.hashCode().toUInt().toString(36)}"
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
            for ((services, module, artifact) in filteredOutputs) {
                module.collectDependencies(services, customWebCompilerSettings).let { (regular, friend) ->
                    regularDependencies += regular
                    friendDependencies += friend
                }
            }

            val maxLanguageVersion = filteredOutputs.maxOf { (_, module, _) ->
                module.languageVersionSettings.languageVersion
            }

            val allLanguageFeatures = filteredOutputs.flatMap { (_, module, _) ->
                module.directives[LanguageSettingsDirectives.LANGUAGE]
            }.distinct()

            val allOptIns = filteredOutputs.flatMap { (_, module, _) ->
                module.directives[LanguageSettingsDirectives.OPT_IN]
            }.distinct()

            val allAllowKotlinPackage = filteredOutputs.any { (_, module, _) ->
                LanguageSettingsDirectives.ALLOW_KOTLIN_PACKAGE in module.directives
            }

            facade.compileSourcesToKlib(
                combinedModule,
                combinedModule.files.filter { it.name.endsWith(".kt") }.map { it.originalFile },
                batchKlibFile,
                languageVersion = maxLanguageVersion.versionString,
                customLanguageFeatures = allLanguageFeatures,
                customOptIns = allOptIns,
                allowKotlinPackage = allAllowKotlinPackage,
                regularDependencies,
                friendDependencies
            )

            // Step 2: Compile KLIB into WASM executable
            val moduleNameHash = someModule.name.hashCode().toHexString()
            val (exitCode, output, executableFolder) = facade.runCli(
                someModule.copy(files = emptyList()),
                moduleNameHash,
                customLanguageFeatures = allLanguageFeatures,
                customOptIns = allOptIns,
                allowKotlinPackage = allAllowKotlinPackage,
                mainLibraries = listOf(batchKlibFile.absolutePath),
                regularDependencies = regularDependencies,
                friendDependencies = friendDependencies,
            )
            if (exitCode == ExitCode.OK) {
                // Copy all additional files to the executable folder
                for (tempFile in tempDir.listFiles() ?: emptyArray()) {
                    if (tempFile.name != "batch.klib" && tempFile.name != "ProxyBatchLauncher.kt") {
                        tempFile.copyTo(executableFolder.resolve(tempFile.name), overwrite = true)
                    }
                }
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
                customOptIns = module.directives[LanguageSettingsDirectives.OPT_IN],
                allowKotlinPackage = LanguageSettingsDirectives.ALLOW_KOTLIN_PACKAGE in module.directives,
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

    private fun applyK2MPPArgs(
        module: TestModule,
        customLanguageFeatures: List<String>
    ): List<String> {
        val isMPP = customLanguageFeatures.any { it == "+MultiPlatformProjects" } ||
                module.directives[LanguageSettingsDirectives.LANGUAGE].contains("+MultiPlatformProjects")

        if (!isMPP) return emptyList()

        val allModules = testServices.moduleStructure.modules
        return buildList {
            allModules.forEach { add("-Xfragments=${it.name}") }

            allModules.forEach { m ->
                m.dependsOnDependencies.forEach { dependency ->
                    add("-Xfragment-refines=${m.name}:${dependency.dependencyModule.name}")
                }
            }

            allModules.forEach { m ->
                m.files.forEach { file ->
                    add("-Xfragment-sources=${m.name}:${file.originalFile.absolutePath}")
                }
            }
        }
    }

    private fun compileSourcesToKlib(
        module: TestModule,
        sources: List<File>,
        klibOutputFile: File,
        languageVersion: String,
        customLanguageFeatures: List<String>,
        customOptIns: List<String>,
        allowKotlinPackage: Boolean,
        regularDependencies: Set<String>,
        friendDependencies: Set<String>,
    ): ExitCode {
        val returnValueCheckerModes: List<ReturnValueCheckerMode> = module.directives[RETURN_VALUE_CHECKER_MODE]
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
                    runIf(allowKotlinPackage) {
                        CommonCompilerArguments::allowKotlinPackage.cliArgument
                    }
                ),
                sources.filter { it.name.endsWith(".kt") }.map { it.absolutePath },
                runIf(regularAndFriendDependencies.isNotEmpty()) {
                    listOf(
                        CommonJsAndWasmCompilerArguments::libraries.cliArgument,
                        regularAndFriendDependencies.joinToString(File.pathSeparator),
                    )
                },
                runIf(friendDependencies.isNotEmpty()) {
                    listOf(CommonJsAndWasmCompilerArguments::friendModules.cliArgument(friendDependencies.joinToString(File.pathSeparator)))
                },
                returnValueCheckerModes.map {
                    CommonCompilerArguments::returnValueChecker.cliArgument(it.state)
                },
                customLanguageFeatures
                    .map { CommonCompilerArguments::manuallyConfiguredFeatures.cliArgument + ":$it" },
                customOptIns.map { CommonCompilerArguments::optIn.cliArgument + "=$it" },
                applyK2MPPArgs(module, customLanguageFeatures),
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
        customOptIns: List<String>,
        allowKotlinPackage: Boolean,
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
                    runIf(allowKotlinPackage) {
                        CommonCompilerArguments::allowKotlinPackage.cliArgument
                    }
                ),
                module.files.filter { it.name.endsWith(".kt") && (it.isAdditional || mainLibraries.isEmpty()) }.map { it.originalFile.absolutePath },
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
                    .map { CommonCompilerArguments::manuallyConfiguredFeatures.cliArgument + ":$it" },
                customOptIns.map { CommonCompilerArguments::optIn.cliArgument + "=$it" },
                applyK2MPPArgs(module, customLanguageFeatures),
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
