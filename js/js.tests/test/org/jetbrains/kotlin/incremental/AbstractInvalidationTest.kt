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
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.common.phaser.toPhaseMap
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.js.klib.generateIrForKlibSerialization
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.ir.backend.js.*
import org.jetbrains.kotlin.ir.backend.js.ic.*
import org.jetbrains.kotlin.ir.backend.js.SourceMapsInfo
import org.jetbrains.kotlin.ir.backend.js.codegen.JsGenerationGranularity
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.CompilationOutputs
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.safeModuleName
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImplForJsIC
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.config.WebConfigurationKeys
import org.jetbrains.kotlin.js.test.converters.ClassicJsBackendFacade
import org.jetbrains.kotlin.js.test.utils.MODULE_EMULATION_FILE
import org.jetbrains.kotlin.js.testOld.V8IrJsTestChecker
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.test.DebugMode
import org.jetbrains.kotlin.test.builders.LanguageVersionSettingsBuilder
import org.jetbrains.kotlin.test.KotlinTestWithEnvironment
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.util.JUnit4Assertions
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import org.junit.ComparisonFailure
import java.io.File
import java.util.EnumSet

abstract class AbstractJsIrInvalidationTest : AbstractInvalidationTest(TargetBackend.JS_IR, "incrementalOut/invalidation")
abstract class AbstractJsIrES6InvalidationTest : AbstractInvalidationTest(TargetBackend.JS_IR_ES6, "incrementalOut/invalidationES6")

abstract class AbstractInvalidationTest(
    private val targetBackend: TargetBackend,
    private val workingDirPath: String
) : KotlinTestWithEnvironment() {
    companion object {
        private val OUT_DIR_PATH = System.getProperty("kotlin.js.test.root.out.dir") ?: error("'kotlin.js.test.root.out.dir' is not set")
        private val STDLIB_KLIB = File(System.getProperty("kotlin.js.stdlib.klib.path") ?: error("Please set stdlib path")).canonicalPath

        private const val BOX_FUNCTION_NAME = "box"
        private const val STDLIB_MODULE_NAME = "kotlin-kotlin-stdlib-js-ir"

        private val TEST_FILE_IGNORE_PATTERN = Regex("^.*\\..+\\.\\w\\w$")

        private val JS_MODULE_KIND = ModuleKind.COMMON_JS
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

    private val File.filesInDir
        get() = listFiles() ?: error("cannot retrieve the file list for $absolutePath directory")

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
        val jsDir = File(workingDir, "js").also { it.invalidateDir() }

        initializeWorkingDir(projectInfo, testDirectory, sourceDir, buildDir)

        ProjectStepsExecutor(projectInfo, modulesInfos, testDirectory, sourceDir, buildDir, jsDir).execute()
    }

    private fun resolveModuleArtifact(moduleName: String, buildDir: File): File {
        return File(File(buildDir, moduleName), "$moduleName.klib")
    }

    private fun createConfiguration(moduleName: String, language: List<String>): CompilerConfiguration {
        val copy = environment.configuration.copy()
        copy.put(CommonConfigurationKeys.MODULE_NAME, moduleName)
        copy.put(JSConfigurationKeys.GENERATE_DTS, true)
        copy.put(JSConfigurationKeys.MODULE_KIND, JS_MODULE_KIND)
        copy.put(WebConfigurationKeys.PROPERTY_LAZY_INITIALIZATION, true)

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
        private val buildDir: File,
        private val jsDir: File
    ) {
        private inner class TestStepInfo(
            val moduleName: String,
            val modulePath: String,
            val friends: List<String>,
            val expectedFileStats: Map<String, Set<String>>,
            val expectedDTS: String?
        )

        private fun setupTestStep(projStep: ProjectInfo.ProjectBuildStep, module: String, buildKlib: Boolean): TestStepInfo {
            val projStepId = projStep.id
            val moduleTestDir = File(testDir, module)
            val moduleSourceDir = File(sourceDir, module)
            val moduleInfo = moduleInfos[module] ?: error("No module info found for $module")
            val moduleStep = moduleInfo.steps[projStepId]
            val deletedFiles = mutableSetOf<String>()
            for (modification in moduleStep.modifications) {
                modification.execute(moduleTestDir, moduleSourceDir) { deletedFiles.add(it.name) }
            }

            val expectedFileStats = moduleStep.expectedFileStats.toMutableMap()
            if (deletedFiles.isNotEmpty()) {
                val removedFiles = expectedFileStats[DirtyFileState.REMOVED_FILE.str] ?: emptySet()
                expectedFileStats[DirtyFileState.REMOVED_FILE.str] = removedFiles + deletedFiles
            }

            val outputKlibFile = resolveModuleArtifact(module, buildDir)

            val friends = mutableListOf<File>()
            if (buildKlib) {
                val dependencies = mutableListOf(File(STDLIB_KLIB))
                for (dep in moduleStep.dependencies) {
                    val klibFile = resolveModuleArtifact(dep.moduleName, buildDir)
                    dependencies += klibFile
                    if (dep.isFriend) {
                        friends += klibFile
                    }
                }
                val configuration = createConfiguration(module, projStep.language)
                buildArtifact(configuration, module, moduleSourceDir, dependencies, friends, outputKlibFile)
            }

            val dtsFile = moduleStep.expectedDTS.ifNotEmpty {
                moduleTestDir.resolve(singleOrNull() ?: error("$module module may generate only one d.ts at step $projStepId"))
            }
            return TestStepInfo(
                module.safeModuleName,
                outputKlibFile.canonicalPath,
                friends.map { it.canonicalPath },
                expectedFileStats,
                dtsFile?.readText()
            )
        }

        private fun verifyCacheUpdateStats(stepId: Int, stats: KotlinSourceFileMap<EnumSet<DirtyFileState>>, testInfo: List<TestStepInfo>) {
            val gotStats = stats.filter { it.key.path != STDLIB_KLIB }

            val checkedLibs = mutableSetOf<KotlinLibraryFile>()

            for (info in testInfo) {
                val libFile = KotlinLibraryFile(info.modulePath)
                val updateStatus = gotStats[libFile] ?: emptyMap()
                checkedLibs += libFile

                val got = mutableMapOf<String, MutableSet<String>>()
                for ((srcFile, dirtyStats) in updateStatus) {
                    for (dirtyStat in dirtyStats) {
                        if (dirtyStat != DirtyFileState.NON_MODIFIED_IR) {
                            got.getOrPut(dirtyStat.str) { mutableSetOf() }.add(File(srcFile.path).name)
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
            val got = gotRebuilt.filter { it != STDLIB_MODULE_NAME }
            JUnit4Assertions.assertSameElements(got, expectedRebuilt) {
                "Mismatched rebuilt modules at step $stepId"
            }
        }

        private fun File.writeAsJsModule(jsCode: String, moduleName: String) {
            writeText(ClassicJsBackendFacade.wrapWithModuleEmulationMarkers(jsCode, JS_MODULE_KIND, moduleName))
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
                    testModuleName = "./$mainModuleName.js",
                    testPackageName = null,
                    testFunctionName = BOX_FUNCTION_NAME,
                    testFunctionArgs = "$stepId",
                    expectedResult = "OK",
                    withModuleSystem = true
                )
            } catch (e: ComparisonFailure) {
                throw ComparisonFailure("Mismatched box out at step $stepId", e.expected, e.actual)
            } catch (e: IllegalStateException) {
                throw IllegalStateException("Something goes wrong (bad JS code?) at step $stepId\n${e.message}")
            }
        }

        private fun verifyDTS(stepId: Int, testInfo: List<TestStepInfo>) {
            for (info in testInfo) {
                val expectedDTS = info.expectedDTS ?: continue

                val dtsFile = jsDir.resolve("${File(info.modulePath).nameWithoutExtension}.d.ts")
                JUnit4Assertions.assertTrue(dtsFile.exists()) {
                    "Cannot find d.ts (${dtsFile.absolutePath}) file for module ${info.moduleName} at step $stepId"
                }

                val gotDTS = dtsFile.readText()
                JUnit4Assertions.assertEquals(expectedDTS, gotDTS) {
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

        private fun writeJsCode(mainModuleName: String, jsOutput: CompilationOutputs): List<String> {
            val compiledJsFiles = jsOutput.writeAll(jsDir, mainModuleName, true, mainModuleName, JS_MODULE_KIND).filter {
                it.extension == "js"
            }
            for (jsCodeFile in compiledJsFiles) {
                jsCodeFile.writeAsJsModule(jsCodeFile.readText(), "./${jsCodeFile.name}")
            }

            return compiledJsFiles.mapTo(prepareExternalJsFiles()) { it.absolutePath }
        }

        fun execute() {
            for (projStep in projectInfo.steps) {
                val testInfo = projStep.order.map { setupTestStep(projStep, it, true) }

                val mainModuleInfo = testInfo.last()
                testInfo.find { it != mainModuleInfo && it.friends.isNotEmpty() }?.let {
                    error("module ${it.moduleName} has friends, but only main module may have the friends")
                }

                val configuration = createConfiguration(projStep.order.last(), projStep.language)
                val cacheUpdater = CacheUpdater(
                    mainModule = mainModuleInfo.modulePath,
                    allModules = testInfo.mapTo(mutableListOf(STDLIB_KLIB)) { it.modulePath },
                    mainModuleFriends = mainModuleInfo.friends,
                    cacheDir = buildDir.resolve("incremental-cache").absolutePath,
                    compilerConfiguration = configuration,
                    irFactory = { IrFactoryImplForJsIC(WholeWorldStageController()) },
                    mainArguments = null,
                    compilerInterfaceFactory = { mainModule, cfg ->
                        JsIrCompilerWithIC(
                            mainModule,
                            cfg,
                            JsGenerationGranularity.PER_MODULE,
                            getPhaseConfig(projStep.id),
                            setOf(FqName(BOX_FUNCTION_NAME)),
                            targetBackend == TargetBackend.JS_IR_ES6
                        )
                    }
                )

                val removedModulesInfo = (projectInfo.modules - projStep.order.toSet()).map { setupTestStep(projStep, it, false) }

                val icCaches = cacheUpdater.actualizeCaches()
                verifyCacheUpdateStats(projStep.id, cacheUpdater.getDirtyFileLastStats(), testInfo + removedModulesInfo)

                val mainModuleName = icCaches.last().moduleExternalName
                val jsExecutableProducer = JsExecutableProducer(
                    mainModuleName = mainModuleName,
                    moduleKind = configuration[JSConfigurationKeys.MODULE_KIND]!!,
                    sourceMapsInfo = SourceMapsInfo.from(configuration),
                    caches = icCaches,
                    relativeRequirePath = true
                )

                val (jsOutput, rebuiltModules) = jsExecutableProducer.buildExecutable(multiModule = true, outJsProgram = true)
                val writtenFiles = writeJsCode(mainModuleName, jsOutput)

                verifyJsExecutableProducerBuildModules(projStep.id, rebuiltModules, projStep.dirtyJS)
                verifyJsCode(projStep.id, mainModuleName, writtenFiles)
                verifyDTS(projStep.id, testInfo)
            }
        }
    }

    private fun String.isAllowedKtFile() = endsWith(".kt") && !TEST_FILE_IGNORE_PATTERN.matches(this)

    private fun String.isAllowedJsFile() = endsWith(".js") && !TEST_FILE_IGNORE_PATTERN.matches(this)

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
        configuration: CompilerConfiguration,
        moduleName: String,
        sourceDir: File,
        dependencies: Collection<File>,
        friends: Collection<File>,
        outputKlibFile: File
    ) {
        if (outputKlibFile.exists()) outputKlibFile.delete()

        val projectJs = environment.project

        val sourceFiles = sourceDir.filteredKtFiles().map { environment.createPsiFile(it) }

        val sourceModule = prepareAnalyzedSourceModule(
            project = projectJs,
            files = sourceFiles,
            configuration = configuration,
            dependencies = dependencies.map { it.canonicalPath },
            friendDependencies = friends.map { it.canonicalPath },
            analyzer = AnalyzerWithCompilerReport(configuration)
        )

        val moduleSourceFiles = (sourceModule.mainModule as MainModule.SourceFiles).files
        val icData = sourceModule.compilerConfiguration.incrementalDataProvider?.getSerializedData(moduleSourceFiles) ?: emptyList()
        val expectDescriptorToSymbol = mutableMapOf<DeclarationDescriptor, IrSymbol>()
        val (moduleFragment, _) = generateIrForKlibSerialization(
            environment.project,
            moduleSourceFiles,
            configuration,
            sourceModule.jsFrontEndResult.analysisResult,
            sortDependencies(sourceModule.moduleDependencies),
            icData,
            expectDescriptorToSymbol,
            IrFactoryImpl,
            verifySignatures = true
        ) {
            sourceModule.getModuleDescriptor(it)
        }
        val metadataSerializer =
            KlibMetadataIncrementalSerializer(configuration, sourceModule.project, sourceModule.jsFrontEndResult.hasErrors)

        generateKLib(
            sourceModule,
            outputKlibFile.canonicalPath,
            nopack = false,
            jsOutputName = moduleName,
            icData = icData,
            expectDescriptorToSymbol = expectDescriptorToSymbol,
            moduleFragment = moduleFragment
        ) { file ->
            metadataSerializer.serializeScope(file, sourceModule.jsFrontEndResult.bindingContext, moduleFragment.descriptor)
        }
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
}
