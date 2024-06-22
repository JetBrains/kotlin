/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.local.CoreLocalFileSystem
import com.intellij.psi.PsiManager
import com.intellij.psi.SingleRootFileViewProvider
import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.common.phaser.toPhaseMap
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoot
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.KlibConfigurationKeys
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.ir.backend.js.JsICContext
import org.jetbrains.kotlin.ir.backend.js.SourceMapsInfo
import org.jetbrains.kotlin.ir.backend.js.ic.*
import org.jetbrains.kotlin.ir.backend.js.jsPhases
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.*
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.test.converters.ClassicJsBackendFacade
import org.jetbrains.kotlin.js.test.utils.MODULE_EMULATION_FILE
import org.jetbrains.kotlin.js.testOld.V8IrJsTestChecker
import org.jetbrains.kotlin.konan.file.ZipFileSystemCacheableAccessor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.multiplatform.isCommonSource
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.test.DebugMode
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.builders.LanguageVersionSettingsBuilder
import org.jetbrains.kotlin.test.util.JUnit4Assertions
import org.jetbrains.kotlin.test.utils.TestDisposable
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.junit.ComparisonFailure
import org.junit.jupiter.api.AfterEach
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.util.*
import java.util.stream.Collectors

abstract class AbstractInvalidationTest(
    private val targetBackend: TargetBackend,
    private val granularity: JsGenerationGranularity,
    private val workingDirPath: String
) {
    companion object {
        private val OUT_DIR_PATH = System.getProperty("kotlin.js.test.root.out.dir") ?: error("'kotlin.js.test.root.out.dir' is not set")
        private val STDLIB_KLIB = File(System.getProperty("kotlin.js.stdlib.klib.path") ?: error("Please set stdlib path")).canonicalPath
        private val KOTLIN_TEST_KLIB = File(System.getProperty("kotlin.js.kotlin.test.klib.path") ?: error("Please set kotlin.test path")).canonicalPath

        private const val BOX_FUNCTION_NAME = "box"
        private const val STDLIB_MODULE_NAME = "kotlin-kotlin-stdlib"
        private const val KOTLIN_TEST_MODULE_NAME = "kotlin-kotlin-test"

        private val TEST_FILE_IGNORE_PATTERN = Regex("^.*\\..+\\.\\w\\w$")

        private const val SOURCE_MAPPING_URL_PREFIX = "//# sourceMappingURL="
    }

    open fun getModuleInfoFile(directory: File): File {
        return directory.resolve(MODULE_INFO_FILE)
    }

    open fun getProjectInfoFile(directory: File): File {
        return directory.resolve(PROJECT_INFO_FILE)
    }

    private val zipAccessor = ZipFileSystemCacheableAccessor(2)

    private val rootDisposable = TestDisposable("${AbstractInvalidationTest::class.simpleName}.rootDisposable")

    protected val environment =
        KotlinCoreEnvironment.createForParallelTests(rootDisposable, CompilerConfiguration(), EnvironmentConfigFiles.JS_CONFIG_FILES)

    @AfterEach
    protected fun disposeEnvironment() {
        // The test is run with `Lifecycle.PER_METHOD` (as it's the default), so the disposable needs to be disposed after each test.
        Disposer.dispose(rootDisposable)
    }

    @AfterEach
    protected fun clearZipAccessor() {
        zipAccessor.reset()
    }

    private fun parseProjectInfo(testName: String, infoFile: File): ProjectInfo {
        return ProjectInfoParser(infoFile).parse(testName)
    }

    private fun parseModuleInfo(moduleName: String, infoFile: File): ModuleInfo {
        return ModuleInfoParser(infoFile).parse(moduleName)
    }

    private val File.filesInDir
        get() = listFiles() ?: error("cannot retrieve the file list for $absolutePath directory")

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

        ProjectStepsExecutor(projectInfo, modulesInfos, testDirectory, sourceDir, buildDir, jsDir).execute()
    }

    private fun resolveModuleArtifact(moduleName: String, buildDir: File): File {
        return File(File(buildDir, moduleName), "$moduleName.klib")
    }

    protected open fun createConfiguration(moduleName: String, language: List<String>, moduleKind: ModuleKind): CompilerConfiguration {
        val copy = environment.configuration.copy()
        copy.put(CommonConfigurationKeys.MODULE_NAME, moduleName)
        copy.put(JSConfigurationKeys.GENERATE_DTS, true)
        copy.put(JSConfigurationKeys.MODULE_KIND, moduleKind)
        copy.put(JSConfigurationKeys.PROPERTY_LAZY_INITIALIZATION, true)
        copy.put(JSConfigurationKeys.SOURCE_MAP, true)
        copy.put(JSConfigurationKeys.USE_ES6_CLASSES, targetBackend == TargetBackend.JS_IR_ES6)
        copy.put(JSConfigurationKeys.COMPILE_SUSPEND_AS_JS_GENERATOR, targetBackend == TargetBackend.JS_IR_ES6)

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

    private inner class ProjectStepsExecutor(
        private val projectInfo: ProjectInfo,
        private val moduleInfos: Map<String, ModuleInfo>,
        private val testDir: File,
        private val sourceDir: File,
        private val buildDir: File,
        private val jsDir: File,
    ) {
        private inner class TestStepInfo(
            val moduleName: String,
            val modulePath: String,
            val friends: List<String>,
            val expectedFileStats: Map<String, Set<String>>,
            val expectedDTS: ExpectedFile?,
        )

        private inner class ExpectedFile(val name: String, val content: String)

        private fun setupTestStep(projStep: ProjectInfo.ProjectBuildStep, module: String): TestStepInfo {
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
                val dependencies = mutableListOf(File(STDLIB_KLIB), File(KOTLIN_TEST_KLIB))
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

        private fun verifyCacheUpdateStats(stepId: Int, stats: KotlinSourceFileMap<EnumSet<DirtyFileState>>, testInfo: List<TestStepInfo>) {
            val gotStats = stats.filter { it.key.path != STDLIB_KLIB && it.key.path != KOTLIN_TEST_KLIB }

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

        private fun verifyJsExecutableProducerBuildModules(stepId: Int, gotRebuilt: List<String>, expectedRebuilt: List<String>) {
            val got = gotRebuilt.filter { !it.startsWith(STDLIB_MODULE_NAME) && !it.startsWith(KOTLIN_TEST_MODULE_NAME) }
            JUnit4Assertions.assertSameElements(got, expectedRebuilt) {
                "Mismatched rebuilt modules at step $stepId"
            }
        }

        private fun File.writeAsJsModule(jsCode: String, moduleName: String) {
            writeText(ClassicJsBackendFacade.wrapWithModuleEmulationMarkers(jsCode, projectInfo.moduleKind, moduleName))
        }

        private fun prepareExternalJsFiles(): MutableList<String> {
            return testDir.filesInDir.mapNotNullTo(mutableListOf(MODULE_EMULATION_FILE)) { file ->
                file.takeIf { it.name.isAllowedJsFile() }?.readText()?.let { jsCode ->
                    val externalModule = jsDir.resolve(file.name)
                    externalModule.writeAsJsModule(jsCode, file.nameWithoutExtension)
                    externalModule.canonicalPath
                }
            }
        }

        private fun verifyJsCode(stepId: Int, mainModuleName: String, jsFiles: List<String>) {
            try {
                V8IrJsTestChecker.checkWithTestFunctionArgs(
                    files = jsFiles,
                    testModuleName = "./$mainModuleName${projectInfo.moduleKind.extension}",
                    testPackageName = null,
                    testFunctionName = BOX_FUNCTION_NAME,
                    testFunctionArgs = "$stepId",
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

        private fun getPhaseConfig(stepId: Int): PhaseConfig {
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
                    JUnit4Assertions.assertEquals("$SOURCE_MAPPING_URL_PREFIX${jsCodeFile.name}.map", sourceMappingUrlLine) {
                        "Mismatched source map url at step $stepId"
                    }
                }

                jsCodeFile.writeAsJsModule(jsCodeFile.readText(), "./${jsCodeFile.name}")
            }

            return compiledJsFiles.mapTo(prepareExternalJsFiles()) { it.absolutePath }
        }

        fun execute() {
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
                    getPhaseConfig(projStep.id),
                    setOf(FqName(BOX_FUNCTION_NAME)),
                )


                val cacheUpdater = CacheUpdater(
                    mainModule = mainModuleInfo.modulePath,
                    allModules = testInfo.mapTo(mutableListOf(STDLIB_KLIB, KOTLIN_TEST_KLIB)) { it.modulePath },
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
        val dir = File(File(File(OUT_DIR_PATH), workingDirPath), testName)

        dir.invalidateDir()

        return dir
    }

    protected fun KotlinCoreEnvironment.createPsiFile(file: File): KtFile {
        val psiManager = PsiManager.getInstance(project)
        val fileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL) as CoreLocalFileSystem

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
