/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.local.CoreLocalFileSystem
import com.intellij.psi.PsiManager
import com.intellij.psi.SingleRootFileViewProvider
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.ir.backend.js.*
import org.jetbrains.kotlin.ir.backend.js.ic.*
import org.jetbrains.kotlin.ir.backend.js.SourceMapsInfo
import org.jetbrains.kotlin.ir.backend.js.codegen.JsGenerationGranularity
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.safeModuleName
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImplForJsIC
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.testOld.V8IrJsTestChecker
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.test.builders.LanguageVersionSettingsBuilder
import org.jetbrains.kotlin.test.KotlinTestWithEnvironment
import org.jetbrains.kotlin.test.util.JUnit4Assertions
import org.junit.ComparisonFailure
import java.io.File
import java.util.EnumSet

abstract class AbstractInvalidationTest : KotlinTestWithEnvironment() {
    companion object {
        private const val TEST_DATA_DIR_PATH = "js/js.translator/testData/"
        private const val BOX_FUNCTION_NAME = "box"
        private const val STDLIB_ALIAS = "stdlib"

        private const val STDLIB_MODULE_NAME = "kotlin-kotlin-stdlib-js-ir"
        private val STDLIB_KLIB = File(System.getProperty("kotlin.js.stdlib.klib.path") ?: error("Please set stdlib path")).canonicalPath

        private val KT_FILE_IGNORE_PATTERN = Regex("^.*\\..+\\.kt$")
    }

    override fun createEnvironment(): KotlinCoreEnvironment {
        return KotlinCoreEnvironment.createForTests(TestDisposable(), CompilerConfiguration(), EnvironmentConfigFiles.JS_CONFIG_FILES)
    }

    private fun parseProjectInfo(testName: String, infoFile: File): ProjectInfo {
        return ProjectInfoParser(infoFile).parse(testName)
    }

    private fun parseModuleInfo(moduleName: String, infoFile: File): ModuleInfo {
        return ModuleInfoParser(infoFile).parse(moduleName)
    }

    protected fun doTest(testPath: String) {
        val testDirectory = File(testPath)
        val testName = testDirectory.name
        val projectInfoFile = File(testDirectory, PROJECT_INFO_FILE)
        val projectInfo = parseProjectInfo(testName, projectInfoFile)

        if (projectInfo.muted) return

        val modulesInfos = mutableMapOf<String, ModuleInfo>()
        for (module in projectInfo.modules) {
            val moduleDirectory = File(testDirectory, module)
            val moduleInfo = File(moduleDirectory, MODULE_INFO_FILE)
            modulesInfos[module] = parseModuleInfo(module, moduleInfo)
        }

        val workingDir = testWorkingDir(projectInfo.name)
        val sourceDir = File(workingDir, "sources").also { it.invalidateDir() }
        val buildDir = File(workingDir, "build").also { it.invalidateDir() }

        initializeWorkingDir(projectInfo, testDirectory, sourceDir, buildDir)

        ProjectStepsExecutor(projectInfo, modulesInfos, testDirectory, sourceDir, buildDir).execute()
    }

    private fun resolveModuleArtifact(moduleName: String, buildDir: File): File {
        return File(File(buildDir, moduleName), "$moduleName.klib")
    }

    private fun resolveModuleCache(moduleName: String, buildDir: File): File {
        return File(File(buildDir, moduleName), "cache")
    }

    private fun createConfiguration(moduleName: String, language: List<String>): CompilerConfiguration {
        val copy = environment.configuration.copy()
        copy.put(CommonConfigurationKeys.MODULE_NAME, moduleName)
        copy.put(JSConfigurationKeys.MODULE_KIND, ModuleKind.PLAIN)
        copy.put(JSConfigurationKeys.PROPERTY_LAZY_INITIALIZATION, true)

        copy.languageVersionSettings = with(LanguageVersionSettingsBuilder()) {
            language.forEach {
                val switchLanguageFeature = when {
                    it.startsWith("+") -> this::enable
                    it.startsWith("-") -> this::disable
                    else -> error("Language feature should start with + or -")
                }
                val feature = LanguageFeature.fromString(it.substring(1)) ?: error("Unknown language feature $it")
                switchLanguageFeature(feature)
            }
            build()
        }
        return copy
    }

    private inner class ProjectStepsExecutor(
        private val projectInfo: ProjectInfo,
        private val moduleInfos: Map<String, ModuleInfo>,
        private val testDir: File,
        private val sourceDir: File,
        private val buildDir: File
    ) {
        private inner class TestStepInfo(
            val moduleName: String,
            val modulePath: String,
            val icCacheDir: String,
            val expectedFileStats: Map<String, Set<String>>
        )

        private fun setupTestStep(projStep: ProjectInfo.ProjectBuildStep, module: String): TestStepInfo {
            val projStepId = projStep.id
            val moduleTestDir = File(testDir, module)
            val moduleSourceDir = File(sourceDir, module)
            val moduleInfo = moduleInfos[module] ?: error("No module info found for $module")
            val moduleStep = moduleInfo.steps[projStepId]
            val deletedFiles = mutableSetOf<String>()
            for (modification in moduleStep.modifications) {
                modification.execute(moduleTestDir, moduleSourceDir) { deletedFiles.add(it.name) }
            }

            val dependencies = moduleStep.dependencies.mapTo(mutableListOf(File(STDLIB_KLIB))) {
                resolveModuleArtifact(it.moduleName, buildDir)
            }
            val outputKlibFile = resolveModuleArtifact(module, buildDir)
            val configuration = createConfiguration(module, projStep.language)
            buildArtifact(configuration, module, moduleSourceDir, dependencies, outputKlibFile)

            val expectedFileStats = if (deletedFiles.isEmpty()) {
                moduleStep.expectedFileStats
            } else {
                moduleStep.expectedFileStats + (DirtyFileState.REMOVED_FILE.str to deletedFiles)
            }
            return TestStepInfo(
                module.safeModuleName,
                outputKlibFile.canonicalPath,
                resolveModuleCache(module, buildDir).canonicalPath,
                expectedFileStats
            )
        }

        private fun verifyCacheUpdateStats(
            stepId: Int, stats: KotlinSourceFileMap<EnumSet<DirtyFileState>>, testInfo: List<TestStepInfo>
        ) {
            val gotStats = stats.filter { it.key.path != STDLIB_KLIB }

            val checkedLibs = mutableSetOf<KotlinLibraryFile>()

            for (info in testInfo) {
                val libFile = KotlinLibraryFile(info.modulePath)
                val updateStatus = gotStats[libFile] ?: emptyMap()
                checkedLibs += libFile

                val got = mutableMapOf<String, MutableSet<String>>()
                for ((srcFile, dirtyStats) in updateStatus) {
                    for (dirtyStat in dirtyStats) {
                        got.getOrPut(dirtyStat.str) { mutableSetOf() }.add(File(srcFile.path).name)
                    }
                }

                JUnit4Assertions.assertSameElements(got.entries, info.expectedFileStats.entries) {
                    "Mismatched file stats for module [${info.moduleName}] at step $stepId"
                }
            }

            for (libFile in gotStats.keys) {
                JUnit4Assertions.assertTrue(libFile in checkedLibs) {
                    "Got unexpected stats for module [${libFile.path}] at step $stepId"
                }
            }
        }

        private fun verifyJsExecutableProducerBuildModules(stepId: Int, gotRebuilt: List<String>, expectedRebuilt: List<String>) {
            val got = gotRebuilt.filter { it != STDLIB_MODULE_NAME }
            JUnit4Assertions.assertSameElements(got, expectedRebuilt) {
                "Mismatched rebuilt modules at step $stepId"
            }
        }

        fun writeJsFile(name: String, text: String): String {
            val file = File(buildDir, "$name.js")
            if (file.exists()) {
                file.delete()
            }
            file.writeText(text)
            return file.canonicalPath
        }

        private fun verifyJsCode(stepId: Int, mainModuleName: String, jsOutput: CompilationOutputs) {
            val files = jsOutput.dependencies.map { writeJsFile(it.first, it.second.jsCode) } + writeJsFile(mainModuleName, jsOutput.jsCode)
            try {
                V8IrJsTestChecker.checkWithTestFunctionArgs(
                    files = files,
                    testModuleName = mainModuleName,
                    testPackageName = null,
                    testFunctionName = BOX_FUNCTION_NAME,
                    testFunctionArgs = "$stepId",
                    expectedResult = "OK",
                    withModuleSystem = false
                )
            } catch (e: ComparisonFailure) {
                throw ComparisonFailure("Mismatched box out at step $stepId", e.expected, e.actual)
            } catch (e: IllegalStateException) {
                throw IllegalStateException("Something goes wrong (bad JS code?) at step $stepId\n${e.message}")
            }
        }

        fun execute() {
            val stdlibCacheDir = resolveModuleCache(STDLIB_ALIAS, buildDir).canonicalPath
            for (projStep in projectInfo.steps) {
                val testInfo = projStep.order.map { setupTestStep(projStep, it) }

                val configuration = createConfiguration(projStep.order.last(), projStep.language)
                val cacheUpdater = CacheUpdater(
                    mainModule = testInfo.last().modulePath,
                    allModules = testInfo.mapTo(mutableListOf(STDLIB_KLIB)) { it.modulePath },
                    icCachePaths = testInfo.mapTo(mutableListOf(stdlibCacheDir)) { it.icCacheDir },
                    compilerConfiguration = configuration,
                    irFactory = { IrFactoryImplForJsIC(WholeWorldStageController()) },
                    mainArguments = null,
                    compilerInterfaceFactory = { mainModule, cfg ->
                        JsIrCompilerWithIC(mainModule, cfg, JsGenerationGranularity.PER_MODULE, setOf(FqName(BOX_FUNCTION_NAME)))
                    }
                )

                val icCaches = cacheUpdater.actualizeCaches()
                verifyCacheUpdateStats(projStep.id, cacheUpdater.getDirtyFileStats(), testInfo)

                val mainModuleName = icCaches.last().moduleExternalName
                val jsExecutableProducer = JsExecutableProducer(
                    mainModuleName = mainModuleName,
                    moduleKind = configuration[JSConfigurationKeys.MODULE_KIND]!!,
                    sourceMapsInfo = SourceMapsInfo.from(configuration),
                    caches = icCaches,
                    relativeRequirePath = true
                )

                val (jsOutput, rebuiltModules) = jsExecutableProducer.buildExecutable(multiModule = true, outJsProgram = true)
                verifyJsExecutableProducerBuildModules(projStep.id, rebuiltModules, projStep.dirtyJS)
                verifyJsCode(projStep.id, mainModuleName, jsOutput)
            }
        }
    }

    private fun String.isAllowedKtFile() = endsWith(".kt") && !KT_FILE_IGNORE_PATTERN.matches(this)

    private fun File.filteredKtFiles(): Collection<File> {
        assert(isDirectory && exists())
        return listFiles { _, name -> name.isAllowedKtFile() }!!.toList()
    }

    private fun KotlinCoreEnvironment.createPsiFile(file: File): KtFile {
        val psiManager = PsiManager.getInstance(project)
        val fileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL) as CoreLocalFileSystem

        val vFile = fileSystem.findFileByIoFile(file) ?: error("File not found: $file")

        return SingleRootFileViewProvider(psiManager, vFile).allFiles.find {
            it is KtFile && it.virtualFile.canonicalPath == vFile.canonicalPath
        } as KtFile
    }

    private fun buildArtifact(
        configuration: CompilerConfiguration, moduleName: String, sourceDir: File, dependencies: Collection<File>, outputKlibFile: File
    ) {
        if (outputKlibFile.exists()) outputKlibFile.delete()

        val projectJs = environment.project

        val sourceFiles = sourceDir.filteredKtFiles().map { environment.createPsiFile(it) }

        val sourceModule = prepareAnalyzedSourceModule(
            projectJs, sourceFiles, configuration, dependencies.map { it.canonicalPath }, emptyList(), // TODO
            AnalyzerWithCompilerReport(configuration)
        )

        generateKLib(sourceModule, IrFactoryImpl, outputKlibFile.canonicalPath, nopack = false, jsOutputName = moduleName)
    }

    private fun initializeWorkingDir(projectInfo: ProjectInfo, testDir: File, sourceDir: File, buildDir: File) {
        for (module in projectInfo.modules) {
            val moduleSourceDir = File(sourceDir, module).also { it.invalidateDir() }
            File(buildDir, module).invalidateDir()
            val testModuleDir = File(testDir, module)

            testModuleDir.listFiles { _, fileName -> fileName.isAllowedKtFile() }!!.forEach { file ->
                assert(!file.isDirectory)
                val fileName = file.name
                val workingFile = File(moduleSourceDir, fileName)
                file.copyTo(workingFile)
            }
        }
    }

    private fun File.invalidateDir() {
        if (exists()) deleteRecursively()
        mkdirs()
    }

    private fun testWorkingDir(testName: String): File {
        val dir = File(File(File(TEST_DATA_DIR_PATH), "incrementalOut/invalidation"), testName)

        dir.invalidateDir()

        return dir
    }
}
