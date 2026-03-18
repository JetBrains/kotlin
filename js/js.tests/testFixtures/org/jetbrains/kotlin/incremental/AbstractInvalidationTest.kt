/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import com.intellij.psi.SingleRootFileViewProvider
import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.backend.common.phaser.then
import org.jetbrains.kotlin.cli.common.allowNoSourceFiles
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoot
import org.jetbrains.kotlin.cli.common.createPerformanceManagerFor
import org.jetbrains.kotlin.cli.common.disposeRootInWriteAction
import org.jetbrains.kotlin.cli.common.localfs.KotlinLocalFileSystem
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.common.renderDiagnosticInternalName
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.pipeline.CheckCompilationErrors
import org.jetbrains.kotlin.cli.pipeline.ConfigurationPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.PipelineContext
import org.jetbrains.kotlin.cli.pipeline.PipelineStepException
import org.jetbrains.kotlin.cli.pipeline.web.WebFir2IrPipelinePhase
import org.jetbrains.kotlin.cli.pipeline.web.WebFrontendPipelinePhase
import org.jetbrains.kotlin.cli.pipeline.web.WebKlibInliningPipelinePhase
import org.jetbrains.kotlin.cli.pipeline.web.WebKlibSerializationPipelinePhase
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.config.phaser.PhaseConfig
import org.jetbrains.kotlin.config.phaser.invokeToplevel
import org.jetbrains.kotlin.diagnostics.impl.DiagnosticsCollectorImpl
import org.jetbrains.kotlin.ir.backend.js.ic.DirtyFileState
import org.jetbrains.kotlin.ir.backend.js.ic.KotlinLibraryFile
import org.jetbrains.kotlin.ir.backend.js.ic.KotlinSourceFileMap
import org.jetbrains.kotlin.js.common.safeModuleName
import org.jetbrains.kotlin.js.config.*
import org.jetbrains.kotlin.js.test.utils.MODULE_EMULATION_FILE
import org.jetbrains.kotlin.js.test.utils.wrapWithModuleEmulationMarkers
import org.jetbrains.kotlin.klib.KlibCompilerInvocationTestUtils
import org.jetbrains.kotlin.konan.file.ZipFileSystemCacheableAccessor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.multiplatform.isCommonSource
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.builders.LanguageVersionSettingsBuilder
import org.jetbrains.kotlin.test.util.JUnit4Assertions
import org.jetbrains.kotlin.test.utils.TestDisposable
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assumptions
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.nio.charset.Charset
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

    protected abstract fun testConfiguration(buildDir: File): KlibCompilerInvocationTestUtils.TestConfiguration

    protected fun runTest(@TestDataFile testPath: String) {
        val testDirectory = ForTestCompileRuntime.transformTestDataPath(testPath)
        val testName = testDirectory.name
        val projectInfoFile = getProjectInfoFile(testDirectory)
        val projectInfo = parseProjectInfo(testName, projectInfoFile)
        Assumptions.assumeTrue(
            InTextDirectivesUtils.isCompatibleTarget(
                /* targetBackend = */ targetBackend,
                /* backends = */ projectInfo.targetBackends.toList(),
                /* doNotTarget = */ emptyList()
            )
        )

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

        testConfiguration(buildDir).run {
            if (isIgnoredTest(projectInfo)) {
                return onIgnoredTest()
            }
        }
        initializeWorkingDir(projectInfo, testDirectory, sourceDir, buildDir)

        createProjectStepsExecutor(projectInfo, modulesInfos, testDirectory, sourceDir, buildDir, jsDir).execute()
    }

    private fun resolveModuleArtifact(moduleName: String, buildDir: File): File {
        return File(File(buildDir, moduleName), "$moduleName.klib")
    }

    protected open fun createConfiguration(
        moduleName: String,
        moduleKind: ModuleKind,
        languageFeatures: List<String>,
        allLibraries: List<String>,
        friendLibraries: List<String>,
        includedLibrary: String? = null,
    ): CompilerConfiguration {
        val copy = environment.configuration.copy()
        copy.moduleName = moduleName
        copy.perModuleOutputName = moduleName
        copy.outputName = moduleName
        copy.moduleKind = moduleKind
        copy.propertyLazyInitialization = true
        copy.sourceMap = true
        copy.useEs6Classes = targetBackend == TargetBackend.JS_IR_ES6
        copy.compileSuspendAsJsGenerator = targetBackend == TargetBackend.JS_IR_ES6
        copy.compileLambdasAsEs6ArrowFunctions = targetBackend == TargetBackend.JS_IR_ES6
        copy.compileLongAsBigint = targetBackend == TargetBackend.JS_IR_ES6

        copy.languageVersionSettings = with(LanguageVersionSettingsBuilder()) {
            languageFeatures.forEach {
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

        copy.libraries = allLibraries
        copy.friendLibraries = friendLibraries
        includedLibrary?.let { copy.includes = includedLibrary }

        zipAccessor.reset()
        copy.put(KlibConfigurationKeys.ZIP_FILE_SYSTEM_ACCESSOR, zipAccessor)
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
                val configuration = createConfiguration(
                    moduleName = module,
                    moduleKind = projectInfo.moduleKind,
                    languageFeatures = projStep.language,
                    allLibraries = dependencies.map { it.canonicalPath },
                    friendLibraries = friends.map { it.canonicalPath },
                )
                configuration.enableKlibRelativePaths(moduleSourceDir)
                outputKlibFile.delete()
                buildKlib(projStepId, buildDir, configuration, moduleSourceDir, outputKlibFile)
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
            val moduleEmulationPath = ForTestCompileRuntime.transformTestDataPath(MODULE_EMULATION_FILE)
            return testDir.filesInDir.mapNotNullTo(mutableListOf(moduleEmulationPath.absolutePath)) { file ->
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

    protected open fun createPhaseConfig(stepId: Int, buildDir: File): PhaseConfig = PhaseConfig()

    private fun buildKlib(
        stepId: Int,
        buildDir: File,
        configuration: CompilerConfiguration,
        sourceDir: File,
        outputKlibFile: File,
    ) {
        val outputStream = ByteArrayOutputStream()
        val messageCollector = PrintingMessageCollector(PrintStream(outputStream), MessageRenderer.PLAIN_FULL_PATHS, true)
        val performanceManager = createPerformanceManagerFor(configuration.targetPlatform ?: error("Expected a target platform"))
        val phaseConfig = createPhaseConfig(stepId, buildDir)

        configuration.messageCollector = messageCollector
        configuration.addSourcesFromDir(sourceDir)
        configuration.produceKlibFile = true
        configuration.outputDir = outputKlibFile.parentFile
        configuration.phaseConfig = phaseConfig
        configuration.renderDiagnosticInternalName = true
        configuration.allowNoSourceFiles = true

        val klibSerializationCompoundPhase = WebFrontendPipelinePhase then
                WebFir2IrPipelinePhase then
                WebKlibInliningPipelinePhase then
                WebKlibSerializationPipelinePhase

        try {
            klibSerializationCompoundPhase.invokeToplevel(
                phaseConfig,
                context = PipelineContext(
                    performanceManager,
                    kaptMode = false,
                ),
                input = ConfigurationPipelineArtifact(configuration, rootDisposable),
            )
        } catch (_: PipelineStepException) {
            // Some pipeline step did not produce any output because of an error.
            // Check for an error below.
        }

        CheckCompilationErrors.CheckDiagnosticCollector.reportToMessageCollector(configuration)

        if (messageCollector.hasErrors()) {
            val messages = outputStream.toByteArray().toString(Charset.forName("UTF-8"))
            throw AssertionError("The following errors occurred serializing test klib:\n$messages")
        }
    }
}
