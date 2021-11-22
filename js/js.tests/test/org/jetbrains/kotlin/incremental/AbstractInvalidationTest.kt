/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.backend.js.generateKLib
import org.jetbrains.kotlin.ir.backend.js.ic.actualizeCacheForModule
import org.jetbrains.kotlin.ir.backend.js.prepareAnalyzedSourceModule
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.KotlinTestWithEnvironment
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

    private fun initializeStdlibCache() {
        return
        if (stdlibCacheDir != null) return
        val cacheDir = createTempDirectory().toFile()

        val configuration = createConfiguration("stdlib")

        actualizeCacheForModule(stdlibKlibPath, cacheDir.canonicalPath, configuration, emptyList(), emptyList(), IrFactoryImpl)

        stdlibCacheDir = cacheDir

    }

    protected fun doTest(testPath: String) {
        initializeConfiguration()
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

        initializeStdlibCache()

        initializeWorkingDir(projectInfo, testDirectory, sourceDir, buildDir)

        executeProjectSteps(projectInfo, modulesInfos, testDirectory, sourceDir, buildDir)
    }

    private val stdlibKlibPath = System.getProperty("kotlin.js.stdlib.klib.path") ?: error("Please set stdlib path")

    private val String.artifactName: String get() = "$this.klib"

    private fun initializeConfiguration() {

    }

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
//                val moduleBuildDir = File(buildDir, module)
                val moduleInfo = moduleInfos[module] ?: error("No module info found for $module")
                for (moduleStep in moduleInfo.steps) {
                    for (modification in moduleStep.modifications) {
                        modification.execute(moduleTestDir, moduleSourceDir)
                    }


                    val dependencies = moduleStep.dependencies.map { resolveModuleArtifact(it, buildDir) }

                    val outputKlibFile = resolveModuleArtifact(module, buildDir)

                    val configuration = createConfiguration(module)

                    buildArtifact(configuration, module, sourceDir, dependencies, outputKlibFile)

                    val dirtyFiles = moduleStep.dirtyFiles.map { File(moduleSourceDir, it) }
                    val icCaches = emptyList<File>() // moduleStep.dependencies.map { resolveModuleCache(it, buildDir) }

                    val moduleCacheDir = resolveModuleCache(module, buildDir)

                    buildCachesAndCheck(configuration, outputKlibFile, moduleCacheDir, dependencies, icCaches, dirtyFiles)
                }
            }
        }
    }

    private fun buildCachesAndCheck(
        configuration: CompilerConfiguration,
        outputKlibFile: File,
        moduleCacheDir: File,
        dependencies: List<File>,
        icCaches: List<File>,
        dirtyFiles: List<File>
    ) {
        // TODO
    }

    private fun KotlinCoreEnvironment.createPsiFile(fileName: String): KtFile {
        val psiManager = PsiManager.getInstance(project)
        val fileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL)

        val file = fileSystem.findFileByPath(fileName) ?: error("File not found: $fileName")

        return psiManager.findFile(file) as KtFile
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

        val sourceFiles = sourceDir.listFiles { _, name -> name.endsWith(".kt") }!!.map {
            environment.createPsiFile(it!!.canonicalPath)
        }

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
        const val PROJECT_INFO_FILE = "project.info"
        const val MODULE_INFO_FILE = "module.info"
        const val TEST_DATA_DIR_PATH = "js/js.translator/testData/"
    }

}