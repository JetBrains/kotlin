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
import org.jetbrains.kotlin.ir.backend.js.*
import org.jetbrains.kotlin.ir.backend.js.ic.*
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsIrLinker
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.SourceMapsInfo
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.safeModuleName
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImplForJsIC
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.testOld.V8IrJsTestChecker
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.test.KotlinTestWithEnvironment
import org.jetbrains.kotlin.test.util.JUnit4Assertions
import org.junit.ComparisonFailure
import java.io.File
import kotlin.io.path.createTempDirectory

abstract class AbstractInvalidationTest : KotlinTestWithEnvironment() {
    companion object {
        private const val TEST_DATA_DIR_PATH = "js/js.translator/testData/"

        private const val stdlibAlias = "stdlib"
        private const val stdlibModuleName = "kotlin"
        private val stdlibKlibPath = File(System.getProperty("kotlin.js.stdlib.klib.path") ?: error("Please set stdlib path")).canonicalPath
        private var stdlibCacheDir: File? = null

        private const val boxFunctionName = "box"
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

    private fun initializeStdlibCache() {
        if (stdlibCacheDir != null) return
        val cacheDir = createTempDirectory().toFile()
        cacheDir.deleteOnExit()

        val cacheUpdater = CacheUpdater(
            stdlibKlibPath,
            listOf(stdlibKlibPath),
            createConfiguration(stdlibAlias),
            listOf(cacheDir.canonicalPath),
            { IrFactoryImplForJsIC(WholeWorldStageController()) },
            null,
            ::buildCacheForModuleFiles
        )
        cacheUpdater.actualizeCaches { updateStatus, updatedModule ->
            JUnit4Assertions.assertEquals(stdlibKlibPath, updatedModule) { "incorrect std klib path" }
            JUnit4Assertions.assertTrue(updateStatus is CacheUpdateStatus.Dirty) { "std klib should be rebuilt" }
        }

        stdlibCacheDir = cacheDir
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

        initializeStdlibCache()

        val workingDir = testWorkingDir(projectInfo.name)
        val sourceDir = File(workingDir, "sources").also { it.invalidateDir() }
        val buildDir = File(workingDir, "build").also { it.invalidateDir() }

        initializeWorkingDir(projectInfo, testDirectory, sourceDir, buildDir)

        ProjectStepsExecutor(projectInfo, modulesInfos, testDirectory, sourceDir, buildDir).execute()
    }

    private fun resolveModuleArtifact(moduleName: String, buildDir: File): File {
        if (moduleName == stdlibAlias) return File(stdlibKlibPath)
        return File(File(buildDir, moduleName), "$moduleName.klib")
    }

    private fun resolveModuleCache(moduleName: String, buildDir: File): File {
        if (moduleName == stdlibAlias) return stdlibCacheDir ?: error("stdlib cache corruption")
        return File(File(buildDir, moduleName), "cache")
    }

    private fun createConfiguration(moduleName: String): CompilerConfiguration {
        val copy = environment.configuration.copy()
        copy.put(CommonConfigurationKeys.MODULE_NAME, moduleName)
        copy.put(JSConfigurationKeys.MODULE_KIND, ModuleKind.PLAIN)
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
            val directives: Set<StepDirectives>,
            val dirtyFiles: List<String> = emptyList(),
            val deletedFiles: List<String> = emptyList()
        )

        private fun setupTestStep(projStepId: Int, module: String): TestStepInfo {
            val moduleTestDir = File(testDir, module)
            val moduleSourceDir = File(sourceDir, module)
            val moduleInfo = moduleInfos[module] ?: error("No module info found for $module")
            val moduleStep = moduleInfo.steps[projStepId]
            val deletedFiles = mutableListOf<File>()
            for (modification in moduleStep.modifications) {
                modification.execute(moduleTestDir, moduleSourceDir) { deletedFiles.add(it) }
            }

            val dependencies = moduleStep.dependencies.map { resolveModuleArtifact(it, buildDir) }
            val outputKlibFile = resolveModuleArtifact(module, buildDir)
            val configuration = createConfiguration(module)
            buildArtifact(configuration, module, moduleSourceDir, dependencies, outputKlibFile)

            return TestStepInfo(
                module.safeModuleName,
                outputKlibFile.canonicalPath,
                resolveModuleCache(module, buildDir).canonicalPath,
                moduleStep.directives,
                moduleStep.dirtyFiles.map { File(moduleSourceDir, it).canonicalPath },
                deletedFiles.map { it.canonicalPath }
            )
        }

        private fun verifyCacheUpdateStatus(stepId: Int, statuses: Map<String, CacheUpdateStatus>, testInfo: List<TestStepInfo>) {
            val expectedInfo = testInfo.filter { StepDirectives.UNUSED_MODULE !in it.directives }

            JUnit4Assertions.assertEquals(statuses.size, expectedInfo.size) {
                "Mismatched updated modules count at step $stepId"
            }

            for (info in expectedInfo) {
                JUnit4Assertions.assertTrue(info.modulePath in statuses) {
                    "Status is missed for ${info.modulePath} at step $stepId"
                }
                val updateStatus = statuses[info.modulePath]!!
                if (StepDirectives.FAST_PATH_UPDATE in info.directives) {
                    JUnit4Assertions.assertEquals(CacheUpdateStatus.FastPath, updateStatus) {
                        "Cache has to be checked by fast path, instead it $updateStatus at step $stepId"
                    }
                } else {
                    JUnit4Assertions.assertNotEquals(CacheUpdateStatus.FastPath, updateStatus) {
                        "Cache has not to be checked by fast path, but it is at step $stepId"
                    }
                }

                val (updated, removed) = when (updateStatus) {
                    is CacheUpdateStatus.FastPath -> emptySet<String>() to emptySet()
                    is CacheUpdateStatus.NoDirtyFiles -> emptySet<String>() to updateStatus.removed
                    is CacheUpdateStatus.Dirty -> updateStatus.updated to updateStatus.removed
                }

                JUnit4Assertions.assertSameElements(info.dirtyFiles, updated.map { File(it).canonicalPath }) {
                    "Mismatched DIRTY files for module ${info.moduleName} at step $stepId"
                }

                JUnit4Assertions.assertSameElements(info.deletedFiles, removed.map { File(it).canonicalPath }) {
                    "Mismatched DELETED files for module ${info.moduleName} at step $stepId"
                }
            }
        }

        private fun verifyJsExecutableProducerBuildModules(stepId: Int, gotRebuilt: Set<String>, expectedRebuilt: List<String>) {
            val expected = expectedRebuilt.map { if (it == stdlibAlias) stdlibModuleName.safeModuleName else it.safeModuleName }
            JUnit4Assertions.assertSameElements(expected, gotRebuilt) {
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
                    testFunctionName = boxFunctionName,
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

        fun executorWithBoxExport(
            currentModule: IrModuleFragment,
            dependencies: Collection<IrModuleFragment>,
            deserializer: JsIrLinker,
            configuration: CompilerConfiguration,
            dirtyFiles: Collection<String>?,
            artifactCache: ArtifactCache,
            exportedDeclarations: Set<FqName>,
            mainArguments: List<String>?
        ) {
            buildCacheForModuleFiles(
                currentModule = currentModule,
                dependencies = dependencies,
                deserializer = deserializer,
                configuration = configuration,
                dirtyFiles = dirtyFiles,
                artifactCache = artifactCache,
                exportedDeclarations = exportedDeclarations + FqName(boxFunctionName),
                mainArguments = mainArguments,
            )
        }

        fun execute() {
            val stdlibInfo = TestStepInfo(
                moduleName = stdlibModuleName.safeModuleName,
                modulePath = stdlibKlibPath,
                icCacheDir = stdlibCacheDir!!.canonicalPath,
                directives = setOf(StepDirectives.FAST_PATH_UPDATE)
            )
            for (projStep in projectInfo.steps) {
                val testInfo = projStep.order.mapTo(mutableListOf(stdlibInfo)) { setupTestStep(projStep.id, it) }

                val configuration = createConfiguration(projStep.order.last())
                val cacheUpdater = CacheUpdater(
                    rootModule = testInfo.last().modulePath,
                    testInfo.map { it.modulePath },
                    configuration,
                    testInfo.map { it.icCacheDir },
                    { IrFactoryImplForJsIC(WholeWorldStageController()) },
                    null,
                    ::executorWithBoxExport
                )

                val updateStatuses = mutableMapOf<String, CacheUpdateStatus>()
                var icCaches = cacheUpdater.actualizeCaches { updateStatus, updatedModule -> updateStatuses[updatedModule] = updateStatus }
                verifyCacheUpdateStatus(projStep.id, updateStatuses, testInfo)

                val mainModuleCacheDir = icCaches.last().artifactsDir!!
                // tests use one stdlib artifact for all cases, patching stdlib cache path here:
                //  - enable using cached js (stdlib) file through all steps in a test case
                //  - disable using cached js (stdlib) file through test cases
                icCaches = icCaches.map {
                    when (it.moduleSafeName) {
                        stdlibModuleName.safeModuleName -> {
                            val stdlibDir = File(mainModuleCacheDir, stdlibAlias).apply { mkdirs() }
                            ModuleArtifact(stdlibModuleName, it.fileArtifacts, stdlibDir, it.forceRebuildJs)
                        }
                        else -> it
                    }
                }

                val jsExecutableProducer = JsExecutableProducer(
                    mainModuleName = testInfo.last().moduleName,
                    moduleKind = configuration[JSConfigurationKeys.MODULE_KIND]!!,
                    sourceMapsInfo = SourceMapsInfo.from(configuration),
                    caches = icCaches,
                    relativeRequirePath = true
                )

                val rebuiltModules = mutableSetOf<String>()
                val jsOutput = jsExecutableProducer.buildExecutable(true) { rebuiltModules += it }
                verifyJsExecutableProducerBuildModules(projStep.id, rebuiltModules, projStep.dirtyJS)
                verifyJsCode(projStep.id, testInfo.last().moduleName, jsOutput)
            }
        }
    }

    private fun File.filteredKtFiles(): Collection<File> {
        assert(isDirectory && exists())
        return listFiles { _, name -> name.endsWith(".kt") }!!.toList()
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
        configuration: CompilerConfiguration,
        moduleName: String,
        sourceDir: File,
        dependencies: Collection<File>,
        outputKlibFile: File
    ) {
        if (outputKlibFile.exists()) outputKlibFile.delete()

        val projectJs = environment.project

        val sourceFiles = sourceDir.filteredKtFiles().map { environment.createPsiFile(it) }

        val sourceModule = prepareAnalyzedSourceModule(
            projectJs,
            sourceFiles,
            configuration,
            dependencies.map { it.canonicalPath },
            emptyList(), // TODO
            AnalyzerWithCompilerReport(configuration)
        )

        generateKLib(sourceModule, IrFactoryImpl, outputKlibFile.canonicalPath, nopack = false, jsOutputName = moduleName)
    }

    private fun initializeWorkingDir(projectInfo: ProjectInfo, testDir: File, sourceDir: File, buildDir: File) {
        for (module in projectInfo.modules) {
            val moduleSourceDir = File(sourceDir, module).also { it.invalidateDir() }
            File(buildDir, module).invalidateDir()
            val testModuleDir = File(testDir, module)

            testModuleDir.listFiles { _, fileName -> fileName.endsWith(".kt") }!!.forEach { file ->
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
