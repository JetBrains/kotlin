/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.common.phaser.toPhaseMap
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.backend.js.JsICContext
import org.jetbrains.kotlin.ir.backend.js.SourceMapsInfo
import org.jetbrains.kotlin.ir.backend.js.ic.*
import org.jetbrains.kotlin.ir.backend.js.getJsPhases
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.*
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.testOld.V8IrJsTestChecker
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.test.DebugMode
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.util.JUnit4Assertions
import org.jetbrains.kotlin.test.utils.TestDisposable
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.junit.ComparisonFailure
import java.io.File

abstract class JsAbstractInvalidationTest(
    targetBackend: TargetBackend,
    granularity: JsGenerationGranularity,
    workingDirPath: String
) : AbstractInvalidationTest(targetBackend, granularity, workingDirPath) {

    companion object {
        protected const val STDLIB_MODULE_NAME = "kotlin-kotlin-stdlib"

        protected const val KOTLIN_TEST_MODULE_NAME = "kotlin-kotlin-test"

        protected const val SOURCE_MAPPING_URL_PREFIX = "//# sourceMappingURL="
    }

    override val targetName: String = "js"

    override val outputDirPath = System.getProperty("kotlin.js.test.root.out.dir") ?: error("'kotlin.js.test.root.out.dir' is not set")

    override val stdlibKLib: String =
        File(System.getProperty("kotlin.js.stdlib.klib.path") ?: error("Please set stdlib path")).canonicalPath

    override val kotlinTestKLib: String =
        File(System.getProperty("kotlin.js.kotlin.test.klib.path") ?: error("Please set kotlin.test path")).canonicalPath

    final override val rootDisposable: TestDisposable =
        TestDisposable("${JsAbstractInvalidationTest::class.simpleName}.rootDisposable")

    override val environment: KotlinCoreEnvironment =
        KotlinCoreEnvironment.createForParallelTests(rootDisposable, CompilerConfiguration(), EnvironmentConfigFiles.JS_CONFIG_FILES)

    override fun createConfiguration(moduleName: String, language: List<String>, moduleKind: ModuleKind): CompilerConfiguration {
        val copy = super.createConfiguration(moduleName, language, moduleKind)
        copy.put(JSConfigurationKeys.USE_ES6_CLASSES, targetBackend == TargetBackend.JS_IR_ES6)
        copy.put(JSConfigurationKeys.COMPILE_SUSPEND_AS_JS_GENERATOR, targetBackend == TargetBackend.JS_IR_ES6)
        return copy
    }

    override fun createProjectStepsExecutor(
        projectInfo: ProjectInfo,
        moduleInfos: Map<String, ModuleInfo>,
        testDir: File,
        sourceDir: File,
        buildDir: File,
        jsDir: File
    ): AbstractProjectStepsExecutor = ProjectStepsExecutor(projectInfo, moduleInfos, testDir, sourceDir, buildDir, jsDir)

    private inner class ProjectStepsExecutor(
        projectInfo: ProjectInfo,
        moduleInfos: Map<String, ModuleInfo>,
        testDir: File,
        sourceDir: File,
        buildDir: File,
        jsDir: File,
    ) : AbstractProjectStepsExecutor(projectInfo, moduleInfos, testDir, sourceDir, buildDir, jsDir) {
        override fun execute() {
            if (granularity in projectInfo.ignoredGranularities) return

            val mainArguments = runIf(projectInfo.callMain) { emptyList<String>() }
            val dtsStrategy = when (granularity) {
                JsGenerationGranularity.PER_FILE -> TsCompilationStrategy.EACH_FILE
                else -> TsCompilationStrategy.MERGED
            }

            for (projStep in projectInfo.steps) {
                val testInfo = projStep.order.map { setupTestStep(projStep, it) }

                val mainModuleInfo = testInfo.last()
                testInfo.find { it != mainModuleInfo && it.friends.isNotEmpty() }?.let {
                    error("module ${it.moduleName} has friends, but only main module may have the friends")
                }

                val configuration = createConfiguration(projStep.order.last(), projStep.language, projectInfo.moduleKind)

                val dirtyData = when (granularity) {
                    JsGenerationGranularity.PER_FILE -> projStep.dirtyJsFiles
                    else -> projStep.dirtyJsModules
                }

                val icContext = JsICContext(
                    mainArguments,
                    granularity,
                    getPhaseConfig(configuration, projStep.id),
                    setOf(FqName(BOX_FUNCTION_NAME)),
                )


                val cacheUpdater = CacheUpdater(
                    mainModule = mainModuleInfo.modulePath,
                    allModules = testInfo.mapTo(mutableListOf(stdlibKLib, kotlinTestKLib)) { it.modulePath },
                    mainModuleFriends = mainModuleInfo.friends,
                    cacheDir = buildDir.resolve("incremental-cache").absolutePath,
                    compilerConfiguration = configuration,
                    icContext = icContext
                )

                val removedModulesInfo = (projectInfo.modules - projStep.order.toSet()).map { setupTestStep(projStep, it) }

                val icCaches = cacheUpdater.actualizeCaches().map { it as JsModuleArtifact }
                verifyCacheUpdateStats(projStep.id, cacheUpdater.getDirtyFileLastStats(), testInfo + removedModulesInfo)

                val mainModuleName = icCaches.last().moduleExternalName
                val jsExecutableProducer = JsExecutableProducer(
                    mainModuleName = mainModuleName,
                    moduleKind = configuration[JSConfigurationKeys.MODULE_KIND]!!,
                    sourceMapsInfo = SourceMapsInfo.from(configuration),
                    caches = icCaches,
                    relativeRequirePath = true
                )

                val (jsOutput, rebuiltModules) = jsExecutableProducer.buildExecutable(granularity, outJsProgram = true)
                val writtenFiles = writeJsCode(projStep.id, mainModuleName, jsOutput, dtsStrategy)

                verifyJsExecutableProducerBuildModules(projStep.id, rebuiltModules, dirtyData)
                verifyJsCode(projStep.id, mainModuleName, writtenFiles)
                verifyDTS(projStep.id, testInfo)
            }
        }

        private fun verifyJsExecutableProducerBuildModules(stepId: Int, gotRebuilt: List<String>, expectedRebuilt: List<String>) {
            val got = gotRebuilt.filter { !it.startsWith(STDLIB_MODULE_NAME) && !it.startsWith(KOTLIN_TEST_MODULE_NAME) }
            JUnit4Assertions.assertSameElements(got, expectedRebuilt) {
                "Mismatched rebuilt modules at step $stepId"
            }
        }

        private fun verifyJsCode(stepId: Int, mainModuleName: String, jsFiles: List<String>) {
            try {
                V8IrJsTestChecker.checkWithTestFunctionArgs(
                    files = jsFiles,
                    testModuleName = "./$mainModuleName${projectInfo.moduleKind.extension}",
                    testPackageName = null,
                    testFunctionName = BOX_FUNCTION_NAME,
                    testFunctionArgs = "$stepId, false",
                    expectedResult = "OK",
                    withModuleSystem = projectInfo.moduleKind in setOf(ModuleKind.COMMON_JS, ModuleKind.UMD, ModuleKind.AMD),
                    entryModulePath = jsFiles.last()
                )
            } catch (e: ComparisonFailure) {
                throw ComparisonFailure("Mismatched box out at step $stepId", e.expected, e.actual)
            } catch (e: IllegalStateException) {
                throw IllegalStateException("Something goes wrong (bad JS code?) at step $stepId\n${e.message}")
            }
        }

        private fun verifyDTS(stepId: Int, testInfo: List<TestStepInfo>) {
            for (info in testInfo) {
                val moduleName = File(info.modulePath).nameWithoutExtension
                val expectedDTS = info.expectedDTS ?: continue
                val dtsFilePath = when (granularity) {
                    JsGenerationGranularity.PER_FILE -> "$moduleName/${expectedDTS.name.substringBefore('.')}.export.d.ts"
                    else -> "$moduleName.d.ts"
                }

                val dtsFile = jsDir.resolve(dtsFilePath)
                JUnit4Assertions.assertTrue(dtsFile.exists()) {
                    "Cannot find d.ts (${dtsFile.absolutePath}) file for module ${info.moduleName} at step $stepId"
                }

                val gotDTS = dtsFile.readText()
                JUnit4Assertions.assertEquals(expectedDTS.content, gotDTS) {
                    "Mismatched d.ts for module ${info.moduleName} at step $stepId"
                }
            }
        }

        private fun getPhaseConfig(configuration: CompilerConfiguration, stepId: Int): PhaseConfig {
            val jsPhases = getJsPhases(configuration)

            if (DebugMode.fromSystemProperty("kotlin.js.debugMode") < DebugMode.SUPER_DEBUG) {
                return PhaseConfig(jsPhases)
            }

            return PhaseConfig(
                jsPhases,
                dumpToDirectory = buildDir.resolve("irdump").resolve("step-$stepId").path,
                toDumpStateAfter = jsPhases.toPhaseMap().values.toSet()
            )
        }

        private fun writeJsCode(
            stepId: Int,
            mainModuleName: String,
            jsOutput: CompilationOutputs,
            dtsStrategy: TsCompilationStrategy
        ): List<String> {
            val compiledJsFiles = jsOutput.writeAll(
                jsDir,
                mainModuleName,
                dtsStrategy,
                mainModuleName,
                projectInfo.moduleKind
            ).filter {
                it.extension == "js" || it.extension == "mjs"
            }
            for (jsCodeFile in compiledJsFiles) {
                val sourceMappingUrlLine = jsCodeFile.readLines().singleOrNull { it.startsWith(SOURCE_MAPPING_URL_PREFIX) }

                if (sourceMappingUrlLine != null) {
                    JUnit4Assertions.assertEquals("${SOURCE_MAPPING_URL_PREFIX}${jsCodeFile.name}.map", sourceMappingUrlLine) {
                        "Mismatched source map url at step $stepId"
                    }
                }

                jsCodeFile.writeAsJsModule(jsCodeFile.readText(), "./${jsCodeFile.name}")
            }

            return compiledJsFiles.mapTo(prepareExternalJsFiles()) { it.absolutePath }
        }
    }
}
