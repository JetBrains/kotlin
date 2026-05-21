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
import org.jetbrains.kotlin.wasm.test.WasmCoroutineHelpersModuleTransformer
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
                // Pick the last non-helpers module as the main module. The helpers module is
                // synthesized by WasmCoroutineHelpersModuleTransformer and contains only the
                // synthetic `helpers` package files, never `box()`.
                val mainModule = testModules.lastOrNull { it.name != WasmCoroutineHelpersModuleTransformer.HELPERS_MODULE_NAME }
                    ?: testModules.last()

                // For isolated tests, `BatchingPackageInserter.processModule()` skips patching when
                //   - the target is Native, OR
                //   - `WITH_REFLECT` is among the global directives, OR
                //   - any source file contains one of the `GroupingTestIsolator.ISOLATION_SOURCE_REGEXES`
                //     patterns (e.g., `// WASM_FAILS_IN: `, `::class.qualifiedName`, `import kotlin.reflect.`).
                // Mirror exactly that decision here so the FQN of `box()` in the generated launcher matches
                // what `BatchingPackageInserter` actually produced for the per-test KLIB.
                val moduleStructure = services.moduleStructure
                val withReflectInGlobalDirectives = JvmEnvironmentConfigurationDirectives.WITH_REFLECT in moduleStructure.allDirectives
                val anyIsolationSourceRegexMatch = org.jetbrains.kotlin.test.model.GroupingTestIsolator.ISOLATION_SOURCE_REGEXES.any { regex ->
                    moduleStructure.modules.any { m -> m.files.any { it.originalContent.contains(regex) } }
                }
                val isPatched = !(withReflectInGlobalDirectives || anyIsolationSourceRegexMatch)

                var fileWithBox: TestFile? = null
                for (module in testModules) {
                    fileWithBox = module.files.firstOrNull {
                        val content = services.sourceFileProvider.getContentOfSourceFile(it)
                        MainFunctionForBlackBoxTestsSourceProvider.containsBoxMethod(content)
                    }
                    if (fileWithBox != null) break
                }

                val isWasiTargetIsolated = mainModule.targetPlatform(testServices).isWasmWasi()

                val batchLauncherFile = if (fileWithBox != null) {
                    val originalPackage = MainFunctionForBlackBoxTestsSourceProvider.detectPackage(fileWithBox)
                    val boxFqName = if (isPatched) {
                        if (originalPackage != null) "$additionalPackage.$originalPackage.box" else "$additionalPackage.box"
                    } else {
                        if (originalPackage != null) "$originalPackage.box" else "box"
                    }
                    val uniqueClassName = BatchingPackageInserter.computeProxyLauncherClassName(services.testInfo)

                    val proxyLauncherContent = buildString {
                        append("import kotlin.test.Test\n")
                        append("import kotlin.test.assertEquals\n\n")
                        append("class $uniqueClassName {\n")
                        append("    @Test\n")
                        append("    fun runTest() {\n")
                        append("        val result = $boxFqName()\n")
                        append("        assertEquals(\"OK\", result, \"Test failed with: \$result\")\n")
                        append("    }\n")
                        append("}\n\n")

                        append("@kotlin.wasm.WasmExport\n")
                        append("fun hasTestFailures(): Boolean {\n")
                        append("    return kotlin.test.hasTestFailures()\n")
                        append("}\n")

                        if (isWasiTargetIsolated) {
                            // WasmEdge/Wasmtime invoke the `startTest` export as the entry point.
                            // For isolated tests we drive the single ProxyLauncher's runTest() and
                            // signal failure via wasiProcExit(1) — same mechanism as the grouped path.
                            append("\n")
                            append("@kotlin.wasm.WasmImport(\"wasi_snapshot_preview1\", \"proc_exit\")\n")
                            append("private external fun wasiProcExit(code: Int)\n")
                            append("\n")
                            append("@kotlin.wasm.WasmExport\n")
                            append("fun startTest() {\n")
                            append("    try {\n")
                            append("        $uniqueClassName().runTest()\n")
                            append("        if (kotlin.test.hasTestFailures()) wasiProcExit(1)\n")
                            append("    } catch (e: Throwable) {\n")
                            append("        println(\"Failed with exception!\")\n")
                            append("        println(e.message)\n")
                            append("        println(e.printStackTrace())\n")
                            append("        wasiProcExit(1)\n")
                            append("    }\n")
                            append("}\n")
                        }
                    }

                    val tempFile = tempDir.resolve("ProxyBatchLauncher.kt")
                    tempFile.writeText(proxyLauncherContent)
                    TestFile("ProxyBatchLauncher.kt", proxyLauncherContent, tempFile, 0, true, mainModule.files.first().directives)
                } else null

                val (regularDependencies, friendDependencies) = mainModule.collectDependencies(services, customWebCompilerSettings)

                // Per-test KLIB paths (the artifacts produced by the NonGroupingStage for this isolated batch).
                val perTestKlibPathsIsolated = filteredOutputs.map { it.third.outputFile.absolutePath }.reversed()

                // Detect tests with friend module dependencies (e.g. `// MODULE: main()(lib1)`). For such
                // tests, the "launcher as -Xinclude main" approach (Option B applied to the isolated path)
                // loses the friend relation between `lib1.klib` and `main.klib` at the IR linking stage —
                // even though both are passed via `-libraries`, neither of them is the included main module
                // anymore, and `-Xfriend-modules` declares friendship only with the *included* module. As a
                // result, virtual dispatch of `internal open` declarations crossing the friend boundary
                // breaks (override is not picked, returning the base implementation). Fall back to the
                // pre-Option-B isolated path which uses the per-test `main.klib` as the included module.
                val hasFriendDependency = testModules.any { module ->
                    module.allDependencies.any { it.relation == org.jetbrains.kotlin.test.model.DependencyRelation.FriendDependency }
                }

                if (batchLauncherFile != null && !hasFriendDependency) {
                    // Apply Option B to the isolated path: compile the synthetic ProxyBatchLauncher.kt
                    // into a small launcher.klib and use it as the included (-Xinclude) main module.
                    // The per-test KLIBs (already produced by NonGroupingStage) are passed as ordinary
                    // -libraries via mainLibraries.drop(1) so that GenerateWasmTests lowers only the
                    // launcher's @Test runTest() (and on WASI also the startTest export).
                    val launcherKlibFile = tempDir.resolve("launcher.klib")
                    val launcherModule = mainModule.copy(files = listOf(batchLauncherFile))
                    facade.compileSourcesToKlib(
                        launcherModule,
                        listOf(batchLauncherFile.originalFile),
                        launcherKlibFile,
                        languageVersion = mainModule.languageVersionSettings.languageVersion.versionString,
                        customLanguageFeatures = mainModule.directives[LanguageSettingsDirectives.LANGUAGE],
                        customOptIns = mainModule.directives[LanguageSettingsDirectives.OPT_IN],
                        allowKotlinPackage = LanguageSettingsDirectives.ALLOW_KOTLIN_PACKAGE in mainModule.directives,
                        regularDependencies + perTestKlibPathsIsolated,
                        friendDependencies
                    )

                    val (exitCode, output, executableFolder) = facade.runCli(
                        mainModule.copy(files = emptyList()),
                        mainModule.name.hashCode().toHexString(),
                        customLanguageFeatures = mainModule.directives[LanguageSettingsDirectives.LANGUAGE],
                        customOptIns = mainModule.directives[LanguageSettingsDirectives.OPT_IN],
                        allowKotlinPackage = LanguageSettingsDirectives.ALLOW_KOTLIN_PACKAGE in mainModule.directives,
                        mainLibraries = listOf(launcherKlibFile.absolutePath) + perTestKlibPathsIsolated,
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
                } else if (batchLauncherFile != null && hasFriendDependency) {
                    // Pre-Option-B isolated path: keep the per-test KLIBs as `mainLibraries` so that one of
                    // them becomes the included main module via `-Xinclude`. The synthetic
                    // ProxyBatchLauncher.kt is passed as an additional source file (`isAdditional = true`)
                    // and gets compiled together with the included module's IR — which preserves the
                    // friend relation between modules of the same multi-module test.
                    val (exitCode, output, executableFolder) = facade.runCli(
                        mainModule.copy(files = listOfNotNull(batchLauncherFile)),
                        mainModule.name.hashCode().toHexString(),
                        customLanguageFeatures = mainModule.directives[LanguageSettingsDirectives.LANGUAGE],
                        customOptIns = mainModule.directives[LanguageSettingsDirectives.OPT_IN],
                        allowKotlinPackage = LanguageSettingsDirectives.ALLOW_KOTLIN_PACKAGE in mainModule.directives,
                        mainLibraries = perTestKlibPathsIsolated,
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
                } else {
                    // No box() in any module of the isolated test: keep the previous behavior of
                    // including one of the per-test KLIBs as the main module (so that lowerings can
                    // still process whatever @Test classes were generated for the per-test sources,
                    // e.g. via WasmJsLauncherAdditionalSourceProvider).
                    val (exitCode, output, executableFolder) = facade.runCli(
                        mainModule.copy(files = emptyList()),
                        mainModule.name.hashCode().toHexString(),
                        customLanguageFeatures = mainModule.directives[LanguageSettingsDirectives.LANGUAGE],
                        customOptIns = mainModule.directives[LanguageSettingsDirectives.OPT_IN],
                        allowKotlinPackage = LanguageSettingsDirectives.ALLOW_KOTLIN_PACKAGE in mainModule.directives,
                        mainLibraries = perTestKlibPathsIsolated,
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
            }

            // Option B: Generate ONLY the ProxyBatchLauncher.kt as the "main" KLIB, and pass
            // all per-test KLIBs (already compiled by NonGroupingStage) as ordinary -libraries.
            // The launcher's @Test-annotated runTest() calls box() in each per-test KLIB via FQN.
            // GenerateWasmTests processes only the main module's IR, which contains the launcher's
            // @Test method — so the test runner picks up exactly one test per per-test KLIB.
            // This avoids the expensive "combine all batch sources into batch.klib" step.

            val isWasiTarget = someModule.targetPlatform(testServices).isWasmWasi()

            val proxyClassNames = mutableListOf<String>()
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

                    val boxFqName = if (originalPackage != null) "$additionalPackage.$originalPackage.box" else "$additionalPackage.box"

                    val uniqueClassName = BatchingPackageInserter.computeProxyLauncherClassName(services.testInfo)
                    proxyClassNames += uniqueClassName
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

                if (isWasiTarget) {
                    // WasmEdge/Wasmtime invoke the `startTest` export as the entry point.
                    // Since this is a grouped batch with many tests, `startTest` here drives all
                    // ProxyLauncher_*.runTest() methods sequentially. We deliberately do NOT
                    // rely on `box()` (there are many of them in different per-test KLIBs) and
                    // do NOT rely on the synthetic `startUnitTests` symbol (it is generated by
                    // the compiler backend and not callable from Kotlin source).
                    append("\n")
                    append("@kotlin.wasm.WasmImport(\"wasi_snapshot_preview1\", \"proc_exit\")\n")
                    append("private external fun wasiProcExit(code: Int)\n")
                    append("\n")
                    append("@kotlin.wasm.WasmExport\n")
                    append("fun startTest() {\n")
                    append("    try {\n")
                    for (className in proxyClassNames) {
                        append("        $className().runTest()\n")
                    }
                    append("        if (kotlin.test.hasTestFailures()) wasiProcExit(1)\n")
                    append("    } catch (e: Throwable) {\n")
                    append("        println(\"Failed with exception!\")\n")
                    append("        println(e.message)\n")
                    append("        println(e.printStackTrace())\n")
                    append("        wasiProcExit(1)\n")
                    append("    }\n")
                    append("}\n")
                }
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

            // Aggregate dependencies and settings from all tests in the batch.
            val regularDependencies = mutableSetOf<String>()
            val friendDependencies = mutableSetOf<String>()
            for ((services, module, _) in filteredOutputs) {
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

            // Per-test KLIBs become regular library dependencies. They contain the box() functions
            // that the launcher will call. They must also be visible to the launcher KLIB compilation.
            //
            // Important: when WITH_COROUTINES is used, each test contributes a separate
            // `helpers.klib` produced by `WasmCoroutineHelpersModuleTransformer`. All such helpers
            // KLIBs are byte-equivalent in this batch context (built from the same synthetic
            // `helpers` package files) and all carry the same KLIB `unique_name` "helpers".
            // We keep only the first one so the linker doesn't fail with
            // `The same 'unique_name=helpers' found in more than one library`.
            //
            // Note: per-test KLIBs may also have OS-specific filenames (e.g. `kt19475-helpers.klib`),
            // but inside they all declare `unique_name=helpers`. We identify them by module name
            // alone, which is constant ("helpers") across the batch.
            val perTestKlibPaths = filteredOutputs
                .distinctBy { (_, module, artifact) ->
                    if (module.name == WasmCoroutineHelpersModuleTransformer.HELPERS_MODULE_NAME) {
                        WasmCoroutineHelpersModuleTransformer.HELPERS_MODULE_NAME
                    } else {
                        artifact.outputFile.absolutePath
                    }
                }
                .map { it.third.outputFile.absolutePath }

            // Filter regularDependencies to drop any helpers.klib paths that belong to other
            // per-test outputs (so we only keep helpers from `someModule`'s dependency closure).
            // After deduplication the final list of libraries passed to the compiler must have
            // at most one helpers.klib path. The remaining helpers.klib that comes from
            // `someModule`'s `collectDependencies` is also a per-test artifact, but it's the
            // only one we keep.
            val helperKlibsInPerTest = filteredOutputs
                .filter { it.second.name == WasmCoroutineHelpersModuleTransformer.HELPERS_MODULE_NAME }
                .map { it.third.outputFile.absolutePath }
                .toSet()
            val keptHelperKlib = perTestKlibPaths.firstOrNull { it in helperKlibsInPerTest }
            val cleanedRegularDependencies = regularDependencies.filterNotTo(mutableSetOf()) { dep ->
                dep in helperKlibsInPerTest && dep != keptHelperKlib
            }

            // Step 1: Compile ONLY the launcher into a small KLIB (a few lines of source, no test sources merged).
            val launcherKlibFile = tempDir.resolve("launcher.klib")
            val launcherModule = someModule.copy(files = listOf(batchLauncherFile))
            facade.compileSourcesToKlib(
                launcherModule,
                listOf(batchLauncherFile.originalFile),
                launcherKlibFile,
                languageVersion = maxLanguageVersion.versionString,
                customLanguageFeatures = allLanguageFeatures,
                customOptIns = allOptIns,
                allowKotlinPackage = allAllowKotlinPackage,
                cleanedRegularDependencies + perTestKlibPaths,
                friendDependencies
            )

            // Step 2: Link the launcher KLIB (as the included "main" module) together with all per-test
            // KLIBs (passed as ordinary -libraries via mainLibraries.drop(1)) into a WASM executable.
            val moduleNameHash = someModule.name.hashCode().toHexString()
            val (exitCode, output, executableFolder) = facade.runCli(
                someModule.copy(files = emptyList()),
                moduleNameHash,
                customLanguageFeatures = allLanguageFeatures,
                customOptIns = allOptIns,
                allowKotlinPackage = allAllowKotlinPackage,
                mainLibraries = listOf(launcherKlibFile.absolutePath) + perTestKlibPaths,
                regularDependencies = cleanedRegularDependencies,
                friendDependencies = friendDependencies,
            )
            if (exitCode == ExitCode.OK) {
                // Copy additional non-Kotlin files (e.g. *.mjs, *.js) from per-test modules to the executable folder.
                for ((services, module, _) in filteredOutputs) {
                    for (file in module.files) {
                        if (file.name.endsWith(".mjs") || file.name.endsWith(".js")) {
                            val content = services.sourceFileProvider.getContentOfSourceFile(file)
                            executableFolder.resolve(file.name).writeText(content)
                        }
                    }
                }
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
