/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental


import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.backend.wasm.*
import org.jetbrains.kotlin.backend.wasm.ic.WasmICContextMultimodule
import org.jetbrains.kotlin.backend.wasm.ic.WasmICContextSingleModule
import org.jetbrains.kotlin.backend.wasm.ic.WasmICContextWholeWorld
import org.jetbrains.kotlin.backend.wasm.lower.markExportedDeclarations
import org.jetbrains.kotlin.cli.create
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.pipeline.web.wasm.WasmCompilationMode
import org.jetbrains.kotlin.cli.pipeline.web.wasm.WasmCompilationMode.Companion.wasmCompilationMode
import org.jetbrains.kotlin.cli.pipeline.web.wasm.compileIncrementallyMultimodule
import org.jetbrains.kotlin.cli.pipeline.web.wasm.compileIncrementallySingleModule
import org.jetbrains.kotlin.cli.pipeline.web.wasm.compileIncrementallyWholeWorld
import org.jetbrains.kotlin.codegen.ModelTarget
import org.jetbrains.kotlin.codegen.ModuleInfo
import org.jetbrains.kotlin.codegen.ProjectInfo
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.targetPlatform
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.backend.js.ic.CacheUpdater
import org.jetbrains.kotlin.ir.backend.js.ic.IrCompilerICInterface
import org.jetbrains.kotlin.ir.backend.js.ic.IrICProgramFragments
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.js.config.*
import org.jetbrains.kotlin.klib.KlibCompilerInvocationTestUtils
import org.jetbrains.kotlin.name.FqName
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

@Suppress("OPT_IN_USAGE")
private fun markExportedDeclarations(dirtyFiles: Collection<IrFile>, context: WasmBackendContext) {
    val testFile = dirtyFiles.firstOrNull { file ->
        file.declarations.any { declaration -> declaration is IrFunction && declaration.name.asString() == "box" }
    } ?: return

    val packageFqName = testFile.packageFqName.asString().takeIf { it.isNotEmpty() }
    markExportedDeclarations(context, testFile, setOf(FqName.fromSegments(listOfNotNull(packageFqName, "box"))))
}


private class WasmICContextMultimoduleForTesting : WasmICContextMultimodule(
    allowIncompleteImplementations = false,
    skipLocalNames = false,
    skipCommentInstructions = false,
    skipLocations = false,
) {
    override fun createCompiler(
        mainModule: IrModuleFragment,
        irBuiltIns: IrBuiltIns,
        configuration: CompilerConfiguration
    ): IrCompilerICInterface = object : WasmCompilerWithICMultimodule(
        mainModule = mainModule,
        irBuiltIns = irBuiltIns,
        configuration = configuration,
        allowIncompleteImplementations = false,
        skipCommentInstructions = false,
        skipLocations = false,
    ) {
        override fun compile(allModules: Collection<IrModuleFragment>, dirtyFiles: Collection<IrFile>): List<() -> IrICProgramFragments> {
            markExportedDeclarations(dirtyFiles, context)
            return super.compile(allModules, dirtyFiles)
        }
    }
}

private class WasmICContextSingleModuleForTesting : WasmICContextSingleModule(
    allowIncompleteImplementations = false,
    skipLocalNames = false,
    skipCommentInstructions = false,
    skipLocations = false,
) {
    override fun createCompiler(
        mainModule: IrModuleFragment,
        irBuiltIns: IrBuiltIns,
        configuration: CompilerConfiguration
    ): IrCompilerICInterface = object : WasmCompilerWithICSingleModule(
        mainModule = mainModule,
        irBuiltIns = irBuiltIns,
        configuration = configuration,
        allowIncompleteImplementations = false,
        skipCommentInstructions = false,
        skipLocations = false,
    ) {
        override fun compile(allModules: Collection<IrModuleFragment>, dirtyFiles: Collection<IrFile>): List<() -> IrICProgramFragments> {
            markExportedDeclarations(dirtyFiles, context)
            return super.compile(allModules, dirtyFiles)
        }
    }
}

private class WasmICContextWholeWorldForTesting : WasmICContextWholeWorld(
    allowIncompleteImplementations = false,
    skipLocalNames = false,
    skipCommentInstructions = false,
    skipLocations = false,
) {
    override fun createCompiler(
        mainModule: IrModuleFragment,
        irBuiltIns: IrBuiltIns,
        configuration: CompilerConfiguration
    ): IrCompilerICInterface = object : WasmCompilerWithICWholeWorld(
        mainModule = mainModule,
        irBuiltIns = irBuiltIns,
        configuration = configuration,
        allowIncompleteImplementations = false,
        skipCommentInstructions = false,
        skipLocations = false,
    ) {
        override fun compile(allModules: Collection<IrModuleFragment>, dirtyFiles: Collection<IrFile>): List<() -> IrICProgramFragments> {
            markExportedDeclarations(dirtyFiles, context)
            return super.compile(allModules, dirtyFiles)
        }
    }
}

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
            val wasmCompilationMode = configuration.wasmCompilationMode()
            val icContext = when (wasmCompilationMode) {
                WasmCompilationMode.MULTI_MODULE -> WasmICContextMultimoduleForTesting()
                WasmCompilationMode.SINGLE_MODULE -> WasmICContextSingleModuleForTesting()
                WasmCompilationMode.REGULAR -> WasmICContextWholeWorldForTesting()
            }

            val cacheUpdater = CacheUpdater(
                cacheDir = cacheDir.absolutePath,
                compilerConfiguration = configuration,
                icContext = icContext,
                checkForClassStructuralChanges = true,
                loadBodiesOnlyForMainModule = wasmCompilationMode == WasmCompilationMode.SINGLE_MODULE,
            )

            val icCaches = cacheUpdater.actualizeCaches()
            if (wasmCompilationMode != WasmCompilationMode.SINGLE_MODULE) {
                verifyCacheUpdateStats(stepId, cacheUpdater.getDirtyFileLastStats(), testInfo + removedModulesInfo)
            }

            val fragmentCompiler = when (wasmCompilationMode) {
                WasmCompilationMode.MULTI_MODULE -> ::compileIncrementallyMultimodule
                WasmCompilationMode.SINGLE_MODULE -> ::compileIncrementallySingleModule
                WasmCompilationMode.REGULAR -> ::compileIncrementallyWholeWorld
            }
            val parametersList = fragmentCompiler(icCaches, configuration)

            parametersList.forEach { parameters ->
                val linkedModule = linkWasmIr(parameters)
                val compilationResult = compileWasmIrToBinary(parameters, linkedModule)
                writeCompilationResult(compilationResult, buildDir, parameters.baseFileName)
            }
        }

        override fun execute() {
            prepareExternalJsFiles()

            for (projStep in projectInfo.steps) {
                val mainModuleName = projStep.order.last()
                val testInfo = projStep.order.map { setupTestStep(projStep, it) }
                val mainModuleInfo = testInfo.last()
                testInfo.find { it != mainModuleInfo && it.friends.isNotEmpty() }?.let {
                    error("module ${it.moduleName} has friends, but only main module may have the friends")
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
