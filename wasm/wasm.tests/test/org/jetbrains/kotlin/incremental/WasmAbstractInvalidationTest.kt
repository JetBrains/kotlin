/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental


import org.jetbrains.kotlin.backend.wasm.compileWasm
import org.jetbrains.kotlin.backend.wasm.ic.WasmICContextForTesting
import org.jetbrains.kotlin.backend.wasm.ir2wasm.WasmCompiledFileFragment
import org.jetbrains.kotlin.backend.wasm.writeCompilationResult
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.codegen.ModelTarget
import org.jetbrains.kotlin.codegen.ModuleInfo
import org.jetbrains.kotlin.codegen.ProjectInfo
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.backend.js.ic.CacheUpdater
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.utils.TestDisposable
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
        File(System.getProperty("kotlin.wasm-js.stdlib.path") ?: error("Please set stdlib path")).canonicalPath

    override val kotlinTestKLib: String =
        File(System.getProperty("kotlin.wasm-js.kotlin.test.path") ?: error("Please set kotlin.test path")).canonicalPath

    final override val rootDisposable: TestDisposable =
        TestDisposable("${WasmAbstractInvalidationTest::class.simpleName}.rootDisposable")

    override val environment: KotlinCoreEnvironment =
        KotlinCoreEnvironment.createForParallelTests(rootDisposable, CompilerConfiguration(), EnvironmentConfigFiles.JS_CONFIG_FILES)

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
            allModules: Collection<String>,
            testInfo: List<TestStepInfo>,
            removedModulesInfo: List<TestStepInfo>,
            commitIncrementalCache: Boolean,
        ) {
            val icContext = WasmICContextForTesting(allowIncompleteImplementations = false, skipLocalNames = false)

            val cacheUpdater = CacheUpdater(
                mainModule = mainModuleInfo.modulePath,
                allModules = allModules,
                mainModuleFriends = mainModuleInfo.friends,
                cacheDir = cacheDir.absolutePath,
                compilerConfiguration = configuration,
                icContext = icContext,
                checkForClassStructuralChanges = true,
                commitIncrementalCache = commitIncrementalCache,
            )

            val icCaches = cacheUpdater.actualizeCaches()
            val fileFragments =
                icCaches.flatMap { it.fileArtifacts }.mapNotNull { it.loadIrFragments()?.mainFragment as? WasmCompiledFileFragment }

            verifyCacheUpdateStats(stepId, cacheUpdater.getDirtyFileLastStats(), testInfo + removedModulesInfo)

            val res = compileWasm(
                wasmCompiledFileFragments = fileFragments,
                moduleName = mainModuleInfo.moduleName,
                configuration = configuration,
                typeScriptFragment = null,
                baseFileName = mainModuleInfo.moduleName,
                emitNameSection = false,
                generateSourceMaps = false,
                generateWat = false
            )

            writeCompilationResult(res, buildDir, mainModuleInfo.moduleName, false)

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


                val configuration = createConfiguration(projStep.order.last(), projStep.language, projectInfo.moduleKind)
                val removedModulesInfo = (projectInfo.modules - projStep.order.toSet()).map { setupTestStep(projStep, it) }
                val allModules = testInfo.mapTo(mutableListOf(stdlibKLib, kotlinTestKLib)) { it.modulePath }

                val cacheDir = buildDir.resolve("incremental-cache")
                val totalSizeBefore =
                    cacheDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()

                compileVerifyAndRun(
                    stepId = projStep.id,
                    cacheDir = cacheDir,
                    mainModuleInfo = mainModuleInfo,
                    configuration = configuration,
                    allModules = allModules,
                    testInfo = testInfo,
                    removedModulesInfo = removedModulesInfo,
                    commitIncrementalCache = false
                )

                val totalSizeAfterNotCommit =
                    cacheDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
                assertEquals(totalSizeBefore, totalSizeAfterNotCommit)

                compileVerifyAndRun(
                    stepId = projStep.id,
                    cacheDir = cacheDir,
                    mainModuleInfo = mainModuleInfo,
                    configuration = configuration,
                    allModules = allModules,
                    testInfo = testInfo,
                    removedModulesInfo = removedModulesInfo,
                    commitIncrementalCache = true
                )
            }
        }
    }
}