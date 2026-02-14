/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental


import com.intellij.util.containers.addIfNotNull
import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.backend.wasm.MultimoduleCompileOptions
import org.jetbrains.kotlin.backend.wasm.WasmIrModuleConfiguration
import org.jetbrains.kotlin.backend.wasm.WasmModuleDependencyImport
import org.jetbrains.kotlin.backend.wasm.compileWasmIrToBinary
import org.jetbrains.kotlin.backend.wasm.ic.WasmICContextForTesting
import org.jetbrains.kotlin.backend.wasm.ic.WasmModuleArtifactMultimodule
import org.jetbrains.kotlin.backend.wasm.ir2wasm.ModuleReferencedDeclarations
import org.jetbrains.kotlin.backend.wasm.ir2wasm.ModuleReferencedTypes
import org.jetbrains.kotlin.backend.wasm.ir2wasm.WasmCompiledCodeFileFragment
import org.jetbrains.kotlin.backend.wasm.ir2wasm.WasmCompiledFileFragment
import org.jetbrains.kotlin.backend.wasm.ir2wasm.hasDeclarations
import org.jetbrains.kotlin.backend.wasm.linkWasmIr
import org.jetbrains.kotlin.backend.wasm.writeCompilationResult
import org.jetbrains.kotlin.cli.create
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.pipeline.web.wasm.encodeModuleName
import org.jetbrains.kotlin.codegen.ModelTarget
import org.jetbrains.kotlin.codegen.ModuleInfo
import org.jetbrains.kotlin.codegen.ProjectInfo
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.targetPlatform
import org.jetbrains.kotlin.ir.backend.js.ic.CacheUpdater
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.js.config.ModuleKind
import org.jetbrains.kotlin.js.config.sourceMap
import org.jetbrains.kotlin.js.config.useDebuggerCustomFormatters
import org.jetbrains.kotlin.js.config.wasmCompilation
import org.jetbrains.kotlin.klib.KlibCompilerInvocationTestUtils
import org.jetbrains.kotlin.platform.wasm.WasmPlatforms
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.services.configuration.WasmEnvironmentConfigurator
import org.jetbrains.kotlin.test.utils.TestDisposable
import org.jetbrains.kotlin.wasm.config.wasmDebug
import org.jetbrains.kotlin.wasm.config.wasmGenerateDwarf
import org.jetbrains.kotlin.wasm.config.wasmGenerateWat
import org.jetbrains.kotlin.wasm.config.wasmTarget
import org.jetbrains.kotlin.wasm.test.AbstractWasmPartialLinkageTestCase
import org.jetbrains.kotlin.wasm.test.WasmCompilerInvocationTestConfiguration
import org.jetbrains.kotlin.wasm.test.tools.WasmVM
import java.io.File
import kotlin.test.assertEquals

abstract class WasmAbstractInvalidationTest(
    targetBackend: TargetBackend,
    workingDirPath: String,
) : AbstractInvalidationTest(targetBackend, workingDirPath) {

    override val modelTarget: ModelTarget = ModelTarget.WASM

    override val outputDirPath = System.getProperty("kotlin.wasm.test.root.out.dir") ?: error("'kotlin.wasm.test.root.out.dir' is not set")

    override val stdlibKLib: String =
        File(WasmEnvironmentConfigurator.stdlibPath(WasmTarget.JS)).canonicalPath

    override val kotlinTestKLib: String =
        File(WasmEnvironmentConfigurator.kotlinTestPath(WasmTarget.JS)).canonicalPath

    final override val rootDisposable: TestDisposable =
        TestDisposable("${WasmAbstractInvalidationTest::class.simpleName}.rootDisposable")

    @OptIn(K1Deprecation::class)
    override val environment: KotlinCoreEnvironment =
        KotlinCoreEnvironment.createForParallelTests(rootDisposable, CompilerConfiguration.create(), EnvironmentConfigFiles.JS_CONFIG_FILES)

    override fun testConfiguration(buildDir: File): KlibCompilerInvocationTestUtils.TestConfiguration =
        WasmCompilerInvocationTestConfiguration(buildDir, AbstractWasmPartialLinkageTestCase.CompilerType.WITH_IC)

    override fun createConfiguration(
        moduleName: String,
        moduleKind: ModuleKind,
        languageFeatures: List<String>,
        allLibraries: List<String>,
        friendLibraries: List<String>,
        includedLibrary: String?,
    ): CompilerConfiguration {
        val config = super.createConfiguration(
            moduleName = moduleName,
            moduleKind = moduleKind,
            languageFeatures = languageFeatures,
            allLibraries = allLibraries,
            friendLibraries = friendLibraries,
            includedLibrary = includedLibrary
        )
        config.wasmCompilation = true
        config.wasmTarget = WasmTarget.JS
        config.targetPlatform = WasmPlatforms.wasmJs
        config.wasmDebug = false
        config.sourceMap = false
        config.useDebuggerCustomFormatters = false
        config.wasmGenerateDwarf = false
        config.wasmGenerateWat = false
        return config
    }

    override fun createProjectStepsExecutor(
        projectInfo: ProjectInfo,
        moduleInfos: Map<String, ModuleInfo>,
        testDir: File,
        sourceDir: File,
        buildDir: File,
        jsDir: File  // Ignored
    ): AbstractProjectStepsExecutor = ProjectStepsExecutor(projectInfo, moduleInfos, testDir, sourceDir, buildDir, buildDir)

    private inner class ProjectStepsExecutor(
        projectInfo: ProjectInfo,
        moduleInfos: Map<String, ModuleInfo>,
        testDir: File,
        sourceDir: File,
        buildDir: File,
        jsDir: File,
    ) : AbstractProjectStepsExecutor(projectInfo, moduleInfos, testDir, sourceDir, buildDir, jsDir) {

        private fun compileVerifyAndRun(
            stepId: Int,
            cacheDir: File,
            mainModuleInfo: TestStepInfo,
            configuration: CompilerConfiguration,
            testInfo: List<TestStepInfo>,
            removedModulesInfo: List<TestStepInfo>,
            commitIncrementalCache: Boolean,
        ) {
            val icContext = WasmICContextForTesting(allowIncompleteImplementations = false, skipLocalNames = false)

            val cacheUpdater = CacheUpdater(
                cacheDir = cacheDir.absolutePath,
                compilerConfiguration = configuration,
                icContext = icContext,
                checkForClassStructuralChanges = true,
                commitIncrementalCache = commitIncrementalCache,
            )

            val icCaches = cacheUpdater.actualizeCaches().filterIsInstance<WasmModuleArtifactMultimodule>()


            parameters.forEach { parameters ->
                val linkedModule = linkWasmIr(parameters)
                val compilationResult = compileWasmIrToBinary(parameters, linkedModule)
                writeCompilationResult(compilationResult, buildDir, parameters.baseFileName)
            }

            WasmVM.NodeJs.run(
                "./test.mjs",
                emptyList(),
                workingDirectory = buildDir,
                useNewExceptionHandling = false,
            )
        }

        override fun execute() {
            prepareExternalJsFiles()

            for (projStep in projectInfo.steps) {
                val testInfo = projStep.order.map { setupTestStep(projStep, it) }
                val mainModuleInfo = testInfo.last()
                testInfo.find { it != mainModuleInfo && it.friends.isNotEmpty() }?.let {
                    error("module ${it.moduleName} has friends, but only main module may have the friends")
                }

                val testRunnerContent = """
                    let boxTestPassed = false;
                    try {
                        let jsModule = await import('./${mainModuleInfo.moduleName}.mjs');
                        jsModule.startUnitTests?.();
                        let result = jsModule.$BOX_FUNCTION_NAME(${projStep.id}, true);
                        if (result.toLowerCase() != "ok") {
                            throw new Error(result);
                        }
                        boxTestPassed = true
                    } catch(e) {
                        console.log('Failed with exception!');
                        console.log(e);
                    }
        
                    if (!boxTestPassed)
                        process.exit(1);
                    """.trimIndent()

                val runnerFile = File(buildDir, "test.mjs")
                runnerFile.writeText(testRunnerContent)


                val configuration = createConfiguration(
                    moduleName = projStep.order.last(),
                    moduleKind = projectInfo.moduleKind,
                    languageFeatures = projStep.language,
                    allLibraries = testInfo.mapTo(mutableListOf(stdlibKLib, kotlinTestKLib)) { it.modulePath },
                    friendLibraries = mainModuleInfo.friends,
                    includedLibrary = mainModuleInfo.modulePath,
                )

                val removedModulesInfo = (projectInfo.modules - projStep.order.toSet()).map { setupTestStep(projStep, it) }

                val cacheDir = buildDir.resolve("incremental-cache")
                val totalSizeBefore =
                    cacheDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()

//                compileVerifyAndRun(
//                    stepId = projStep.id,
//                    cacheDir = cacheDir,
//                    mainModuleInfo = mainModuleInfo,
//                    configuration = configuration,
//                    testInfo = testInfo,
//                    removedModulesInfo = removedModulesInfo,
//                    commitIncrementalCache = false
//                )

                val totalSizeAfterNotCommit =
                    cacheDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
                assertEquals(totalSizeBefore, totalSizeAfterNotCommit)

                compileVerifyAndRun(
                    stepId = projStep.id,
                    cacheDir = cacheDir,
                    mainModuleInfo = mainModuleInfo,
                    configuration = configuration,
                    testInfo = testInfo,
                    removedModulesInfo = removedModulesInfo,
                    commitIncrementalCache = true
                )
            }
        }
    }

}
