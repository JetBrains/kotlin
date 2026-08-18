/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental


import org.jetbrains.kotlin.CoreEnvironmentDeprecation
import org.jetbrains.kotlin.backend.wasm.*
import org.jetbrains.kotlin.backend.wasm.ic.*
import org.jetbrains.kotlin.backend.wasm.lower.markFunctionToExport
import org.jetbrains.kotlin.cli.create
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.pipeline.ConfigurationPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.cli.pipeline.web.WasmIntermediatePipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.web.WebIncrementalCachePipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.web.wasm.*
import org.jetbrains.kotlin.cli.pipeline.web.wasm.WasmCompilationMode.Companion.wasmCompilationMode
import org.jetbrains.kotlin.codegen.ModelTarget
import org.jetbrains.kotlin.codegen.ModuleInfo
import org.jetbrains.kotlin.codegen.ProjectInfo
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.targetPlatform
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.backend.js.ic.ModuleArtifact
import org.jetbrains.kotlin.ir.backend.js.ic.PlatformDependentICContext
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.types.isBoolean
import org.jetbrains.kotlin.ir.types.isInt
import org.jetbrains.kotlin.ir.types.isString
import org.jetbrains.kotlin.js.config.*
import org.jetbrains.kotlin.klib.KlibCompilerInvocationTestUtils
import org.jetbrains.kotlin.platform.wasm.WasmPlatforms
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.services.configuration.WasmEnvironmentConfigurator
import org.jetbrains.kotlin.test.testInfraError
import org.jetbrains.kotlin.test.utils.TestDisposable
import org.jetbrains.kotlin.wasm.config.wasmDebug
import org.jetbrains.kotlin.wasm.config.wasmGenerateDwarf
import org.jetbrains.kotlin.wasm.config.wasmGenerateWat
import org.jetbrains.kotlin.wasm.config.wasmTarget
import org.jetbrains.kotlin.wasm.test.AbstractWasmPartialLinkageTestCase
import org.jetbrains.kotlin.wasm.test.WasmCompilerInvocationTestConfiguration
import org.jetbrains.kotlin.wasm.test.WasmIcTest
import org.jetbrains.kotlin.wasm.test.tools.WasmVM
import java.io.File

@Suppress("OPT_IN_USAGE")
private fun markExportedDeclarations(dirtyFiles: Collection<IrFile>, context: WasmBackendContext) {
    dirtyFiles.forEach { file ->
        markFunctionToExport(context, file) {
            // fun box(): String
            // fun box(step: Int): String
            // fun box(stepId: Int, isWasm: Boolean): String
            name.asString() == "box" &&
                    parameters.let {
                        it.isEmpty() || it.size == 1 && it[0].type.isInt() || it.size == 2 && it[0].type.isInt() && it[1].type.isBoolean()
                    } &&
                    returnType.isString()
        }
    }
}

object WasmMultiModuleIncrementalCachePreparationPipelinePhaseForTesting :
    WasmIncrementalCachePreparationPipelinePhase<WasmModuleArtifactMultimodule, WasmICContextMultimodule>(
        name = "WasmMultiModuleIncrementalCachePreparationPipelinePhaseForTesting",
        contextFactory = { _, _, _, _ -> WasmICContextMultimoduleForTesting() },
    )

private class WasmICContextMultimoduleForTesting : WasmICContextMultimodule(
    allowIncompleteImplementations = false,
    skipLocalNames = false,
    skipCommentInstructions = false,
    skipLocations = false,
) {
    override fun createCompiler(
        mainModule: IrModuleFragment,
        irBuiltIns: IrBuiltIns,
        configuration: CompilerConfiguration,
        context: WasmBackendContext,
    ): WasmCompilerWithICMultimodule = object : WasmCompilerWithICMultimodule(
        mainModule = mainModule,
        allowIncompleteImplementations = false,
        skipCommentInstructions = false,
        skipLocations = false,
        context = context,
    ) {
        override fun compile(
            allModules: Collection<IrModuleFragment>,
            dirtyFiles: Collection<IrFile>,
        ): List<() -> WasmIrProgramFragmentsMultimodule> {
            markExportedDeclarations(dirtyFiles, super.context)
            return super.compile(allModules, dirtyFiles)
        }
    }
}

object WasmSingleModuleIncrementalCachePreparationPipelinePhaseForTesting :
    WasmIncrementalCachePreparationPipelinePhase<WasmModuleArtifactSingleModule, WasmICContextSingleModule>(
        name = "WasmSingleModuleIncrementalCachePreparationPipelinePhaseForTesting",
        contextFactory = { _, _, _, _ -> WasmICContextSingleModuleForTesting() },
    )

private class WasmICContextSingleModuleForTesting : WasmICContextSingleModule(
    allowIncompleteImplementations = false,
    skipLocalNames = false,
    skipCommentInstructions = false,
    skipLocations = false,
) {
    override fun createCompiler(
        mainModule: IrModuleFragment,
        irBuiltIns: IrBuiltIns,
        configuration: CompilerConfiguration,
        context: WasmBackendContext,
    ): WasmCompilerWithICSingleModule = object : WasmCompilerWithICSingleModule(
        mainModule = mainModule,
        allowIncompleteImplementations = false,
        skipCommentInstructions = false,
        skipLocations = false,
        context = context,
    ) {
        override fun compile(
            allModules: Collection<IrModuleFragment>,
            dirtyFiles: Collection<IrFile>,
        ): List<() -> WasmIrProgramFragmentsSingleModule> {
            markExportedDeclarations(dirtyFiles, super.context)
            return super.compile(allModules, dirtyFiles)
        }
    }
}

object WasmWholeWorldIncrementalCachePreparationPipelinePhaseForTesting :
    WasmIncrementalCachePreparationPipelinePhase<WasmModuleArtifact, WasmICContextWholeWorld>(
        name = "WasmWholeWorldIncrementalCachePreparationPipelinePhaseForTesting",
        contextFactory = { _, _, _, _ -> WasmICContextWholeWorldForTesting() },
    )

private class WasmICContextWholeWorldForTesting : WasmICContextWholeWorld(
    allowIncompleteImplementations = false,
    skipLocalNames = false,
    skipCommentInstructions = false,
    skipLocations = false,
) {
    override fun createCompiler(
        mainModule: IrModuleFragment,
        irBuiltIns: IrBuiltIns,
        configuration: CompilerConfiguration,
        context: WasmBackendContext,
    ): WasmCompilerWithICWholeWorld = object : WasmCompilerWithICWholeWorld(
        mainModule = mainModule,
        allowIncompleteImplementations = false,
        skipCommentInstructions = false,
        skipLocations = false,
        context = context,
    ) {
        override fun compile(allModules: Collection<IrModuleFragment>, dirtyFiles: Collection<IrFile>): List<() -> WasmIrProgramFragments> {
            markExportedDeclarations(dirtyFiles, super.context)
            return super.compile(allModules, dirtyFiles)
        }
    }
}

@WasmIcTest
abstract class WasmAbstractInvalidationTest(
    targetBackend: TargetBackend,
    workingDirPath: String,
) : AbstractInvalidationTest(targetBackend, workingDirPath) {

    override val modelTarget: ModelTarget = ModelTarget.WASM

    override val outputDirPath = System.getProperty("kotlin.wasm.test.root.out.dir") ?: testInfraError("'kotlin.wasm.test.root.out.dir' is not set")

    override val stdlibKLib: String =
        File(WasmEnvironmentConfigurator.stdlibPath(WasmTarget.JS)).canonicalPath

    override val kotlinTestKLib: String =
        File(WasmEnvironmentConfigurator.kotlinTestPath(WasmTarget.JS)).canonicalPath

    final override val rootDisposable: TestDisposable =
        TestDisposable("${WasmAbstractInvalidationTest::class.simpleName}.rootDisposable")

    @OptIn(CoreEnvironmentDeprecation::class)
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
        outputDir: File,
    ): CompilerConfiguration {
        val config = super.createConfiguration(
            moduleName = moduleName,
            moduleKind = moduleKind,
            languageFeatures = languageFeatures,
            allLibraries = allLibraries,
            friendLibraries = friendLibraries,
            includedLibrary = includedLibrary,
            outputDir = outputDir
        )
        config.wasmCompilation = true
        config.wasmTarget = WasmTarget.JS
        config.targetPlatform = WasmPlatforms.wasmJs
        config.wasmDebug = false
        config.sourceMap = false
        config.useDebuggerCustomFormatters = false
        config.wasmGenerateDwarf = false
        config.wasmGenerateWat = false
        config.outputName = moduleName
        modifyConfig(config)
        return config
    }

    protected open fun modifyConfig(configuration: CompilerConfiguration) {}

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
        private fun compileAndVerify(
            stepId: Int,
            cacheDir: File,
            configuration: CompilerConfiguration,
            testInfo: List<TestStepInfo>,
            removedModulesInfo: List<TestStepInfo>,
        ) {
            configuration.icCacheDirectory = cacheDir.absolutePath
            val wasmCompilationMode = configuration.wasmCompilationMode()
            configuration.artifactConfigurations = listOf(
                WebArtifactConfiguration.fromFlags(
                    configuration,
                    isPerModule = false,
                    isPerFile = false,
                    generateDts = false
                )!!,
            )

            fun <M : ModuleArtifact, C : PlatformDependentICContext<M, *, *, *>> runPipeline(
                icCachePreparationPhase: WasmIncrementalCachePreparationPipelinePhase<M, C>,
                incrementalBuildingPhase: PipelinePhase<WebIncrementalCachePipelineArtifact<M>, WasmIntermediatePipelineArtifact>,
            ) {
                val preparedIcCachesArtifact =
                    icCachePreparationPhase.executePhase(ConfigurationPipelineArtifact(configuration, rootDisposable))!!

                if (wasmCompilationMode != WasmCompilationMode.SINGLE_MODULE) {
                    verifyCacheUpdateStats(stepId, preparedIcCachesArtifact.dirtyFileLastStats, testInfo + removedModulesInfo)
                }

                val [parametersList] = incrementalBuildingPhase.executePhase(preparedIcCachesArtifact)!!

                parametersList.forEach { parameters ->
                    val linkedModule = linkWasmIr(parameters)
                    val compilationResult = compileWasmIrToBinary(parameters, linkedModule)
                    writeCompilationResult(compilationResult, buildDir, parameters.baseFileName)
                }
            }

            when (wasmCompilationMode) {
                WasmCompilationMode.MULTI_MODULE -> runPipeline(
                    WasmMultiModuleIncrementalCachePreparationPipelinePhaseForTesting,
                    WasmMultiModuleIncrementalBuildingPhase,
                )
                WasmCompilationMode.SINGLE_MODULE -> runPipeline(
                    WasmSingleModuleIncrementalCachePreparationPipelinePhaseForTesting,
                    WasmSingleModuleIncrementalBuildingPhase,
                )
                WasmCompilationMode.REGULAR -> runPipeline(
                    WasmWholeWorldIncrementalCachePreparationPipelinePhaseForTesting,
                    WasmWholeWorldIncrementalBuildingPhase,
                )
            }
        }

        override fun execute() {
            prepareExternalJsFiles()

            for (projStep in projectInfo.steps) {
                val mainModuleName = projStep.order.last()
                val testInfo = projStep.order.map { setupTestStep(projStep, it) }
                val mainModuleInfo = testInfo.last()
                testInfo.find { it != mainModuleInfo && it.friends.isNotEmpty() }?.let {
                    testInfraError("module ${it.moduleName} has friends, but only main module may have the friends")
                }

                val testRunnerContent = """
                    let boxTestPassed = false;
                    try {
                        let jsModule = await import('./$mainModuleName.mjs');
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
                    moduleName = mainModuleName,
                    moduleKind = projectInfo.moduleKind,
                    languageFeatures = projStep.language,
                    allLibraries = testInfo.mapTo(mutableListOf(stdlibKLib, kotlinTestKLib)) { it.modulePath },
                    friendLibraries = mainModuleInfo.friends,
                    includedLibrary = mainModuleInfo.modulePath,
                    outputDir = jsDir,
                )

                val removedModulesInfo = (projectInfo.modules - projStep.order.toSet()).map { setupTestStep(projStep, it) }

                val cacheDir = buildDir.resolve("incremental-cache")

                compileVerifyAndRun(
                    stepId = projStep.id,
                    cacheDir = cacheDir,
                    configuration = configuration,
                    testInfo = testInfo,
                    removedModulesInfo = removedModulesInfo,
                )
            }
        }

        private fun compileVerifyAndRun(
            stepId: Int,
            cacheDir: File,
            configuration: CompilerConfiguration,
            testInfo: List<TestStepInfo>,
            removedModulesInfo: List<TestStepInfo>,
        ) {
            if (configuration.wasmCompilationMode() == WasmCompilationMode.SINGLE_MODULE) {
                val allLibraries = configuration.libraries
                allLibraries.forEach { currentLib ->
                    configuration.includes = currentLib
                    configuration.libraries = allLibraries.filter { it != currentLib } + currentLib
                    val currentCacheDir = cacheDir.resolve(currentLib.hashCode().toString())
                    compileAndVerify(
                        stepId = stepId,
                        cacheDir = currentCacheDir,
                        configuration = configuration,
                        testInfo = testInfo,
                        removedModulesInfo = removedModulesInfo,
                    )
                }
            } else {
                compileAndVerify(
                    stepId = stepId,
                    cacheDir = cacheDir,
                    configuration = configuration,
                    testInfo = testInfo,
                    removedModulesInfo = removedModulesInfo,
                )
            }

            WasmVM.NodeJs.run(
                "./test.mjs",
                emptyList(),
                workingDirectory = buildDir,
                useNewExceptionHandling = false,
            )
        }
    }
}
