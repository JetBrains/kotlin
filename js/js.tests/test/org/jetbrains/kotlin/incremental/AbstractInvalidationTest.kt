/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import com.intellij.psi.SingleRootFileViewProvider
import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoot
import org.jetbrains.kotlin.cli.common.localfs.KotlinLocalFileSystem
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.ir.backend.js.ic.DirtyFileState
import org.jetbrains.kotlin.ir.backend.js.ic.KotlinLibraryFile
import org.jetbrains.kotlin.ir.backend.js.ic.KotlinSourceFileMap
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.safeModuleName
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.test.utils.MODULE_EMULATION_FILE
import org.jetbrains.kotlin.js.test.utils.wrapWithModuleEmulationMarkers
import org.jetbrains.kotlin.konan.file.ZipFileSystemCacheableAccessor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.multiplatform.isCommonSource
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.builders.LanguageVersionSettingsBuilder
import org.jetbrains.kotlin.test.util.JUnit4Assertions
import org.jetbrains.kotlin.cli.common.disposeRootInWriteAction
import org.jetbrains.kotlin.test.utils.TestDisposable
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import org.junit.jupiter.api.AfterEach
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.util.*
import java.util.stream.Collectors

abstract class AbstractInvalidationTest(
    protected val targetBackend: TargetBackend,
    private val workingDirPath: String,
) {
    protected abstract val modelTarget: ModelTarget

    protected abstract val outputDirPath: String

    protected abstract val stdlibKLib: String

    protected abstract val kotlinTestKLib: String

    companion object {
        @JvmStatic
        protected val BOX_FUNCTION_NAME = "box"

        protected val TEST_FILE_IGNORE_PATTERN = Regex("^.*\\..+\\.\\w\\w$")
    }

    open fun getModuleInfoFile(directory: File): File {
        return directory.resolve(MODULE_INFO_FILE)
    }

    open fun getProjectInfoFile(directory: File): File {
        return directory.resolve(PROJECT_INFO_FILE)
    }

    private val zipAccessor = ZipFileSystemCacheableAccessor(2)

    protected abstract val rootDisposable: TestDisposable

    protected abstract val environment: KotlinCoreEnvironment

    @AfterEach
    protected fun disposeEnvironment() {
        // The test is run with `Lifecycle.PER_METHOD` (as it's the default), so the disposable needs to be disposed after each test.
        disposeRootInWriteAction(rootDisposable)
    }

    @AfterEach
    protected fun clearZipAccessor() {
        zipAccessor.reset()
    }

    private fun parseProjectInfo(testName: String, infoFile: File): ProjectInfo {
        return ProjectInfoParser(infoFile, modelTarget).parse(testName)
    }

    private fun parseModuleInfo(moduleName: String, infoFile: File): ModuleInfo {
        return ModuleInfoParser(infoFile, modelTarget).parse(moduleName)
    }

    private val File.filesInDir
        get() = listFiles() ?: error("cannot retrieve the file list for $absolutePath directory")

    protected abstract fun createProjectStepsExecutor(
        projectInfo: ProjectInfo,
        moduleInfos: Map<String, ModuleInfo>,
        testDir: File,
        sourceDir: File,
        buildDir: File,
        jsDir: File,
    ): AbstractProjectStepsExecutor

    protected fun runTest(@TestDataFile testPath: String) {
        val testDirectory = File(testPath)
        val testName = testDirectory.name
        val projectInfoFile = getProjectInfoFile(testDirectory)
        val projectInfo = parseProjectInfo(testName, projectInfoFile)

        if (isIgnoredTest(projectInfo)) {
            return
        }

        val modulesInfos = mutableMapOf<String, ModuleInfo>()
        for (module in projectInfo.modules) {
            val moduleDirectory = File(testDirectory, module)
            val moduleInfo = getModuleInfoFile(moduleDirectory)
            modulesInfos[module] = parseModuleInfo(module, moduleInfo)
        }

        val workingDir = testWorkingDir(projectInfo.name)
        val sourceDir = File(workingDir, "sources").also { it.invalidateDir() }
        val buildDir = File(workingDir, "build").also { it.invalidateDir() }
        val jsDir = File(workingDir, "js").also { it.invalidateDir() }

        initializeWorkingDir(projectInfo, testDirectory, sourceDir, buildDir)

        createProjectStepsExecutor(projectInfo, modulesInfos, testDirectory, sourceDir, buildDir, jsDir).execute()
    }

    private fun resolveModuleArtifact(moduleName: String, buildDir: File): File {
        return File(File(buildDir, moduleName), "$moduleName.klib")
    }

    protected open fun createConfiguration(moduleName: String, language: List<String>, moduleKind: ModuleKind): CompilerConfiguration {
        val copy = environment.configuration.copy()
        copy.put(CommonConfigurationKeys.MODULE_NAME, moduleName)
        copy.put(JSConfigurationKeys.MODULE_KIND, moduleKind)
        copy.put(JSConfigurationKeys.PROPERTY_LAZY_INITIALIZATION, true)
        copy.put(JSConfigurationKeys.SOURCE_MAP, true)
        copy.put(JSConfigurationKeys.USE_ES6_CLASSES, targetBackend == TargetBackend.JS_IR_ES6)
        copy.put(JSConfigurationKeys.COMPILE_SUSPEND_AS_JS_GENERATOR, targetBackend == TargetBackend.JS_IR_ES6)
        copy.put(JSConfigurationKeys.COMPILE_LAMBDAS_AS_ES6_ARROW_FUNCTIONS, targetBackend == TargetBackend.JS_IR_ES6)

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

        zipAccessor.reset()
        copy.put(JSConfigurationKeys.ZIP_FILE_SYSTEM_ACCESSOR, zipAccessor)
        return copy
    }

    private fun CompilerConfiguration.enableKlibRelativePaths(moduleSourceDir: File) {
        val bases = mutableListOf<String>()
        val platformDirs = moduleSourceDir.listFiles() ?: arrayOf()
        for (platformDir in platformDirs) {
            if (platformDir.isDirectory) {
                bases.add(platformDir.absolutePath)
            }
        }
        if (bases.isEmpty()) {
            bases.add(moduleSourceDir.absolutePath)
        }
        put(KlibConfigurationKeys.KLIB_RELATIVE_PATH_BASES, bases)
    }

    protected abstract inner class AbstractProjectStepsExecutor(
        protected val projectInfo: ProjectInfo,
        private val moduleInfos: Map<String, ModuleInfo>,
        private val testDir: File,
        private val sourceDir: File,
        protected val buildDir: File,
        protected val jsDir: File,
    ) {
        protected inner class TestStepInfo(
            val moduleName: String,
            val modulePath: String,
            val friends: List<String>,
            val expectedFileStats: Map<String, Set<String>>,
            val expectedDTS: ExpectedFile?,
        )

        protected inner class ExpectedFile(val name: String, val content: String)

        protected fun setupTestStep(projStep: ProjectInfo.ProjectBuildStep, module: String): TestStepInfo {
            val projStepId = projStep.id
            val moduleTestDir = File(testDir, module)
            val moduleSourceDir = File(sourceDir, module)
            val moduleInfo = moduleInfos[module] ?: error("No module info found for $module")
            val moduleStep = moduleInfo.steps.getValue(projStepId)
            for (modification in moduleStep.modifications) {
                modification.execute(moduleTestDir, moduleSourceDir) {}
            }

            val outputKlibFile = resolveModuleArtifact(module, buildDir)

            val friends = mutableListOf<File>()
            if (moduleStep.rebuildKlib) {
                val dependencies = mutableListOf(File(stdlibKLib), File(kotlinTestKLib))
                for (dep in moduleStep.dependencies) {
                    val klibFile = resolveModuleArtifact(dep.moduleName, buildDir)
                    dependencies += klibFile
                    if (dep.isFriend) {
                        friends += klibFile
                    }
                }
                val configuration = createConfiguration(module, projStep.language, projectInfo.moduleKind)
                configuration.enableKlibRelativePaths(moduleSourceDir)
                outputKlibFile.delete()
                buildKlib(configuration, module, moduleSourceDir, dependencies, friends, outputKlibFile)
            }

            val dtsFile = moduleStep.expectedDTS.ifNotEmpty {
                moduleTestDir.resolve(singleOrNull() ?: error("$module module may generate only one d.ts at step $projStepId"))
            }
            return TestStepInfo(
                module.safeModuleName,
                outputKlibFile.canonicalPath,
                friends.map { it.canonicalPath },
                moduleStep.expectedFileStats,
                dtsFile?.let { ExpectedFile(moduleStep.expectedDTS.single(), it.readText()) }
            )
        }

        protected fun verifyCacheUpdateStats(
            stepId: Int,
            stats: KotlinSourceFileMap<EnumSet<DirtyFileState>>,
            testInfo: List<TestStepInfo>
        ) {
            val gotStats = stats.filter { it.key.path != stdlibKLib && it.key.path != kotlinTestKLib }

            val checkedLibs = mutableSetOf<KotlinLibraryFile>()

            for (info in testInfo) {
                val libFile = KotlinLibraryFile(info.modulePath)
                val updateStatus = gotStats[libFile] ?: emptyMap()
                checkedLibs += libFile

                val got = mutableMapOf<String, MutableSet<String>>()
                for ((srcFile, dirtyStats) in updateStatus) {
                    for (dirtyStat in dirtyStats) {
                        if (dirtyStat != DirtyFileState.NON_MODIFIED_IR) {
                            got.getOrPut(dirtyStat.str) { mutableSetOf() }.add(srcFile.toString())
                        }
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

        protected fun File.writeAsJsModule(jsCode: String, moduleName: String) {
            writeText(wrapWithModuleEmulationMarkers(jsCode, projectInfo.moduleKind, moduleName))
        }

        protected fun prepareExternalJsFiles(): MutableList<String> {
            return testDir.filesInDir.mapNotNullTo(mutableListOf(MODULE_EMULATION_FILE)) { file ->
                file.takeIf { it.name.isAllowedJsFile() }?.readText()?.let { jsCode ->
                    val externalModule = jsDir.resolve(file.name)
                    externalModule.writeAsJsModule(jsCode, file.nameWithoutExtension)
                    externalModule.canonicalPath
                }
            }
        }

        abstract fun execute()
    }

    private fun String.isAllowedKtFile() = endsWith(".kt") && !TEST_FILE_IGNORE_PATTERN.matches(this)

    private fun String.isAllowedJsFile() = (endsWith(".js") || endsWith(".mjs")) && !TEST_FILE_IGNORE_PATTERN.matches(this)

    protected fun CompilerConfiguration.addSourcesFromDir(sourceDir: File): List<KtFile> {
        assert(sourceDir.isDirectory && sourceDir.exists()) { "Cannot find source directory $sourceDir" }

        val sourceFiles = Files.find(sourceDir.toPath(), Integer.MAX_VALUE, { path: Path, fileAttributes: BasicFileAttributes ->
            fileAttributes.isRegularFile && "${path.fileName}".isAllowedKtFile()
        }).map { it.toFile() }.collect(Collectors.toList())

        val ktSources = mutableListOf<KtFile>()
        for (sourceFile in sourceFiles) {
            val isCommon = sourceFile.parentFile.name == "common"
            addKotlinSourceRoot(sourceFile.absolutePath, isCommon)
            val ktFile = environment.createPsiFile(sourceFile)
            ktFile.isCommonSource = isCommon
            ktSources.add(ktFile)
        }
        return ktSources
    }

    private fun initializeWorkingDir(projectInfo: ProjectInfo, testDir: File, sourceDir: File, buildDir: File) {
        for (module in projectInfo.modules) {
            val moduleSourceDir = File(sourceDir, module).also { it.invalidateDir() }
            File(buildDir, module).invalidateDir()
            val testModuleDir = File(testDir, module)

            testModuleDir.filesInDir.forEach { file ->
                if (file.name.isAllowedKtFile()) {
                    file.copyTo(moduleSourceDir.resolve(file.name))
                }
            }
        }
    }

    private fun File.invalidateDir() {
        if (exists()) deleteRecursively()
        mkdirs()
    }

    private fun testWorkingDir(testName: String): File {
        val dir = File(File(File(outputDirPath), workingDirPath), testName)

        dir.invalidateDir()

        return dir
    }

    private fun KotlinCoreEnvironment.createPsiFile(file: File): KtFile {
        val psiManager = PsiManager.getInstance(project)
        val fileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL) as KotlinLocalFileSystem

        val vFile = fileSystem.findFileByIoFile(file) ?: error("File not found: $file")

        return SingleRootFileViewProvider(psiManager, vFile).allFiles.find {
            it is KtFile && it.virtualFile.canonicalPath == vFile.canonicalPath
        } as KtFile
    }

    protected open fun isIgnoredTest(projectInfo: ProjectInfo) = projectInfo.muted

    protected abstract fun buildKlib(
        configuration: CompilerConfiguration,
        moduleName: String,
        sourceDir: File,
        dependencies: Collection<File>,
        friends: Collection<File>,
        outputKlibFile: File,
    )
}
