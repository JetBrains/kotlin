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
import org.jetbrains.kotlin.ir.backend.js.generateKLib
import org.jetbrains.kotlin.ir.backend.js.ic.CacheUpdateStatus
import org.jetbrains.kotlin.ir.backend.js.ic.PersistentCacheConsumer
import org.jetbrains.kotlin.ir.backend.js.ic.actualizeCacheForModule
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsIrLinker
import org.jetbrains.kotlin.ir.backend.js.prepareAnalyzedSourceModule
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.KotlinTestWithEnvironment
import org.jetbrains.kotlin.test.util.JUnit4Assertions
import java.io.File
import kotlin.io.path.createTempDirectory

private var stdlibCacheDir: File? = null

abstract class AbstractInvalidationTest : KotlinTestWithEnvironment() {
    override fun createEnvironment(): KotlinCoreEnvironment {
        return KotlinCoreEnvironment.createForTests(TestDisposable(), CompilerConfiguration(), EnvironmentConfigFiles.JS_CONFIG_FILES)
    }

    private fun parseProjectInfo(testName: String, infoFile: File): ProjectInfo {
        return ProjectInfoParser(infoFile).parse(testName)
    }

    private fun parseModuleInfo(moduleName: String, infoFile: File): ModuleInfo {
        return ModuleInfoParser(infoFile).parse(moduleName)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun emptyChecker(
        currentModule: IrModuleFragment,
        dependencies: Collection<IrModuleFragment>,
        deserializer: JsIrLinker,
        configuration: CompilerConfiguration,
        dirtyFiles: Collection<String>?, // if null consider the whole module dirty
        deletedFiles: Collection<String>,
        cacheConsumer: PersistentCacheConsumer,
        exportedDeclarations: Set<FqName>,
        mainArguments: List<String>?,
    ) { }

    private fun initializeStdlibCache() {
        if (stdlibCacheDir != null) return
        val cacheDir = createTempDirectory().toFile()
        cacheDir.deleteOnExit()

        val configuration = createConfiguration("stdlib")

        actualizeCacheForModule(stdlibKlibPath, cacheDir.canonicalPath, configuration, listOf(stdlibKlibPath), emptyList(), IrFactoryImpl, ::emptyChecker)

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

        executeProjectSteps(projectInfo, modulesInfos, testDirectory, sourceDir, buildDir)
    }

    private val stdlibKlibPath = System.getProperty("kotlin.js.stdlib.klib.path") ?: error("Please set stdlib path")

    private val String.artifactName: String get() = "$this.klib"

    private fun resolveModuleArtifact(moduleName: String, buildDir: File): File {
        if (moduleName == "stdlib") return File(stdlibKlibPath)
        return File(File(buildDir, moduleName), moduleName.artifactName)
    }

    private fun resolveModuleCache(moduleName: String, buildDir: File): File {
        if (moduleName == "stdlib") return stdlibCacheDir ?: error("stdlib cache corruption")
        return File(File(buildDir, moduleName), "cache")
    }

    private fun createConfiguration(moduleName: String): CompilerConfiguration {
        val copy = environment.configuration.copy()
        copy.put(CommonConfigurationKeys.MODULE_NAME, moduleName)
        return copy
    }

    private fun executeProjectSteps(
        projectInfo: ProjectInfo,
        moduleInfos: Map<String, ModuleInfo>,
        testDir: File,
        sourceDir: File,
        buildDir: File
    ) {
        for (projStep in projectInfo.steps) {
            for (module in projStep.order) {
                val moduleTestDir = File(testDir, module)
                val moduleSourceDir = File(sourceDir, module)
                val moduleInfo = moduleInfos[module] ?: error("No module info found for $module")
                val moduleStep = moduleInfo.steps[projStep.id]
                val deletedFiles = mutableListOf<File>()
                for (modification in moduleStep.modifications) {
                    modification.execute(moduleTestDir, moduleSourceDir) { deletedFiles.add(it) }
                }

                val dependencies = moduleStep.dependencies.map { resolveModuleArtifact(it, buildDir) }

                val outputKlibFile = resolveModuleArtifact(module, buildDir)

                val configuration = createConfiguration(module)

                buildArtifact(configuration, module, moduleSourceDir, dependencies, outputKlibFile)

                val dirtyFiles = moduleStep.dirtyFiles.map { File(moduleSourceDir, it) }
                val icCaches = moduleStep.dependencies.map { resolveModuleCache(it, buildDir) }

                val moduleCacheDir = resolveModuleCache(module, buildDir)

                buildCachesAndCheck(moduleStep, configuration, moduleSourceDir, outputKlibFile, moduleCacheDir, dependencies, icCaches, dirtyFiles, deletedFiles)
            }
        }
    }

    private fun File.filteredKtFiles(): Collection<File> {
        assert(isDirectory && exists())
        return listFiles { _, name -> name.endsWith(".kt") }!!.toList()
    }

    private fun buildCachesAndCheck(
        moduleStep: ModuleInfo.ModuleStep,
        configuration: CompilerConfiguration,
        sourceDir: File,
        moduleKlibFile: File,
        moduleCacheDir: File,
        dependencies: List<File>,
        icCaches: List<File>,
        expectedDirtyFiles: List<File>,
        expectedDeletedFiles: List<File>
    ) {
        @Suppress("UNUSED_PARAMETER")
        fun dirtyFilesChecker(
            currentModule: IrModuleFragment,
            dependencies: Collection<IrModuleFragment>,
            deserializer: JsIrLinker,
            configuration: CompilerConfiguration,
            invalidatedDirtyFiles: Collection<String>?, // if null consider the whole module dirty
            deletedFiles: Collection<String>,
            cacheConsumer: PersistentCacheConsumer,
            exportedDeclarations: Set<FqName>,
            mainArguments: List<String>?,
        ) {
            val actualDirtyFiles =
                invalidatedDirtyFiles?.map { File(it).canonicalPath } ?: sourceDir.filteredKtFiles().map { it.canonicalPath }
            val expectedDirtyFilesCanonical = expectedDirtyFiles.map { it.canonicalPath }

            JUnit4Assertions.assertSameElements(expectedDirtyFilesCanonical, actualDirtyFiles) {
                "Mismatched DIRTY files for module $moduleKlibFile at step ${moduleStep.id}"
            }

            val actualDeletedFiles =
                deletedFiles.map { File(it).canonicalPath }
            val expectedDeletedFilesCanonical = expectedDeletedFiles.map { it.canonicalPath }

            JUnit4Assertions.assertSameElements(expectedDeletedFilesCanonical, actualDeletedFiles) {
                "Mismatched DELETED files for module $moduleKlibFile at step ${moduleStep.id}"
            }
        }

        val dependenciesPaths = mutableListOf<String>()
        dependencies.mapTo(dependenciesPaths) { it.canonicalPath }
        dependenciesPaths.add(moduleKlibFile.canonicalPath)

        val updateStatus = actualizeCacheForModule(
            moduleKlibFile.canonicalPath,
            moduleCacheDir.canonicalPath,
            configuration,
            dependenciesPaths,
            icCaches.map { it.canonicalPath },// + moduleCacheDir.canonicalPath,
            IrFactoryImpl,
            ::dirtyFilesChecker
        )

        if (StepDirectives.FAST_PATH_UPDATE in moduleStep.directives) {
            JUnit4Assertions.assertEquals(CacheUpdateStatus.FAST_PATH, updateStatus) {
                "Cache has to be checked by fast path, instead it $updateStatus"
            }
        }

        JUnit4Assertions.assertEquals(expectedDirtyFiles.isEmpty(), updateStatus.upToDate) {
            val filePaths = expectedDirtyFiles.joinToString(",", "[", "]")
            "Up to date is not expected for module $moduleKlibFile at step ${moduleStep.id}. Expected dirtyFiles are $filePaths"
        }
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

    companion object {
        const val TEST_DATA_DIR_PATH = "js/js.translator/testData/"
    }

}