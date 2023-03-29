/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.ir

import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.local.CoreLocalFileSystem
import com.intellij.psi.PsiManager
import com.intellij.psi.SingleRootFileViewProvider
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.js.klib.compileModuleToAnalyzedFir
import org.jetbrains.kotlin.cli.js.klib.generateIrForKlibSerialization
import org.jetbrains.kotlin.cli.js.klib.serializeFirKlib
import org.jetbrains.kotlin.cli.js.klib.transformFirToIr
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.ir.backend.js.*
import org.jetbrains.kotlin.ir.backend.js.codegen.JsGenerationGranularity
import org.jetbrains.kotlin.ir.backend.js.ic.CacheUpdater
import org.jetbrains.kotlin.ir.backend.js.ic.JsExecutableProducer
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.CompilationOutputs
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.IrModuleToJsTransformer
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.TranslationMode
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImplForJsIC
import org.jetbrains.kotlin.ir.linkage.partial.PartialLinkageConfig
import org.jetbrains.kotlin.ir.linkage.partial.PartialLinkageLogLevel
import org.jetbrains.kotlin.ir.linkage.partial.PartialLinkageMode
import org.jetbrains.kotlin.ir.linkage.partial.setupPartialLinkageConfig
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.testOld.V8IrJsTestChecker
import org.jetbrains.kotlin.klib.PartialLinkageTestUtils
import org.jetbrains.kotlin.klib.PartialLinkageTestUtils.Dependencies
import org.jetbrains.kotlin.klib.PartialLinkageTestUtils.MAIN_MODULE_NAME
import org.jetbrains.kotlin.konan.file.ZipFileSystemCacheableAccessor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.nio.charset.Charset
import kotlin.io.path.createTempDirectory

abstract class AbstractJsPartialLinkageNoICTestCase : AbstractJsPartialLinkageTestCase(CompilerType.K1_NO_IC)
abstract class AbstractJsPartialLinkageWithICTestCase : AbstractJsPartialLinkageTestCase(CompilerType.K1_WITH_IC)
abstract class AbstractFirJsPartialLinkageNoICTestCase : AbstractJsPartialLinkageTestCase(CompilerType.K2_NO_IC)

abstract class AbstractJsPartialLinkageTestCase(val compilerType: CompilerType) : KtUsefulTestCase() {
    enum class CompilerType(val testModeName: String) {
        K1_NO_IC("JS_NO_IC"),
        K1_WITH_IC("JS_WITH_IC"),
        K2_NO_IC("JS_NO_IC")
    }

    private lateinit var buildDir: File
    private lateinit var environment: KotlinCoreEnvironment

    private val zipAccessor = ZipFileSystemCacheableAccessor(2)

    override fun setUp() {
        super.setUp()
        buildDir = createTempDirectory().toFile().also { it.mkdirs() }

        environment = KotlinCoreEnvironment.createForTests(
            testRootDisposable,
            CompilerConfiguration(),
            EnvironmentConfigFiles.JS_CONFIG_FILES
        )
    }

    override fun tearDown() {
        zipAccessor.reset()
        buildDir.deleteRecursively()
        super.tearDown()
    }

    private fun createConfig(moduleName: String): CompilerConfiguration {
        val config = environment.configuration.copy()
        config.put(CommonConfigurationKeys.MODULE_NAME, moduleName)
        config.put(JSConfigurationKeys.MODULE_KIND, ModuleKind.PLAIN)
        config.put(JSConfigurationKeys.PROPERTY_LAZY_INITIALIZATION, true)

        zipAccessor.reset()
        config.put(JSConfigurationKeys.ZIP_FILE_SYSTEM_ACCESSOR, zipAccessor)

        return config
    }

    private inner class JsTestConfiguration(testPath: String) : PartialLinkageTestUtils.TestConfiguration {
        override val testDir: File = File(testPath).absoluteFile
        override val buildDir: File get() = this@AbstractJsPartialLinkageTestCase.buildDir
        override val stdlibFile: File get() = File("libraries/stdlib/js-ir/build/classes/kotlin/js/main").absoluteFile
        override val testModeName get() = this@AbstractJsPartialLinkageTestCase.compilerType.testModeName

        override fun buildKlib(
            moduleName: String,
            moduleSourceDir: File,
            dependencies: Dependencies,
            klibFile: File
        ) = this@AbstractJsPartialLinkageTestCase.buildKlib(moduleName, moduleSourceDir, dependencies, klibFile)

        override fun buildBinaryAndRun(mainModuleKlibFile: File, dependencies: Dependencies) =
            this@AbstractJsPartialLinkageTestCase.buildBinaryAndRun(mainModuleKlibFile, dependencies)

        override fun onNonEmptyBuildDirectory(directory: File) {
            zipAccessor.reset()
            directory.listFiles()?.forEach(File::deleteRecursively)
        }

        override fun onIgnoredTest() {
            /* Do nothing specific. JUnit 3 does not support programmatic tests muting. */
        }
    }

    // The entry point to generated test classes.
    fun doTest(testPath: String) = PartialLinkageTestUtils.runTest(JsTestConfiguration(testPath))

    private fun buildKlib(moduleName: String, moduleSourceDir: File, dependencies: Dependencies, klibFile: File) {
        when (compilerType) {
            CompilerType.K1_NO_IC, CompilerType.K1_WITH_IC -> buildKlibWithK1(moduleName, moduleSourceDir, dependencies, klibFile)
            CompilerType.K2_NO_IC -> buildKlibWithK2(moduleName, moduleSourceDir, dependencies, klibFile)
        }
    }

    private fun buildKlibWithK1(moduleName: String, moduleSourceDir: File, dependencies: Dependencies, klibFile: File) {
        val config = createConfig(moduleName)
        val ktFiles = environment.createPsiFiles(moduleSourceDir)

        val regularDependencies = dependencies.regularDependencies.map { it.libraryFile.absolutePath }
        val friendDependencies = dependencies.friendDependencies.map { it.libraryFile.absolutePath }

        val moduleStructure = prepareAnalyzedSourceModule(
            environment.project,
            ktFiles,
            config,
            regularDependencies,
            friendDependencies,
            AnalyzerWithCompilerReport(config)
        )

        val moduleSourceFiles = (moduleStructure.mainModule as MainModule.SourceFiles).files
        val icData = moduleStructure.compilerConfiguration.incrementalDataProvider?.getSerializedData(moduleSourceFiles) ?: emptyList()
        val expectDescriptorToSymbol = mutableMapOf<DeclarationDescriptor, IrSymbol>()
        val (moduleFragment, _) = generateIrForKlibSerialization(
            environment.project,
            moduleSourceFiles,
            config,
            moduleStructure.jsFrontEndResult.jsAnalysisResult,
            sortDependencies(moduleStructure.moduleDependencies),
            icData,
            expectDescriptorToSymbol,
            IrFactoryImpl,
            verifySignatures = true
        ) {
            moduleStructure.getModuleDescriptor(it)
        }

        val metadataSerializer =
            KlibMetadataIncrementalSerializer(config, moduleStructure.project, moduleStructure.jsFrontEndResult.hasErrors)

        generateKLib(
            moduleStructure,
            klibFile.path,
            nopack = false,
            jsOutputName = moduleName,
            icData = icData,
            expectDescriptorToSymbol = expectDescriptorToSymbol,
            moduleFragment = moduleFragment
        ) { file ->
            metadataSerializer.serializeScope(file, moduleStructure.jsFrontEndResult.bindingContext, moduleFragment.descriptor)
        }
    }

    private fun buildKlibWithK2(moduleName: String, moduleSourceDir: File, dependencies: Dependencies, klibFile: File) {
        val config = createConfig(moduleName)
        val ktFiles = environment.createPsiFiles(moduleSourceDir)

        val regularDependencies = dependencies.regularDependencies.map { it.libraryFile.absolutePath }
        val friendDependencies = dependencies.friendDependencies.map { it.libraryFile.absolutePath }

        val diagnosticsReporter = DiagnosticReporterFactory.createPendingReporter()

        val moduleStructure = ModulesStructure(
            project = environment.project,
            mainModule = MainModule.SourceFiles(ktFiles),
            compilerConfiguration = config,
            dependencies = regularDependencies,
            friendDependenciesPaths = friendDependencies
        )

        val outputStream = ByteArrayOutputStream()
        val messageCollector = PrintingMessageCollector(PrintStream(outputStream), MessageRenderer.PLAIN_FULL_PATHS, true)

        val outputs = compileModuleToAnalyzedFir(
            moduleStructure = moduleStructure,
            ktFiles = ktFiles,
            libraries = regularDependencies,
            friendLibraries = friendDependencies,
            messageCollector = messageCollector,
            diagnosticsReporter = diagnosticsReporter
        )

        if (outputs != null) {
            val fir2IrActualizedResult = transformFirToIr(moduleStructure, outputs, diagnosticsReporter)

            serializeFirKlib(
                moduleStructure = moduleStructure,
                firOutputs = outputs,
                fir2IrActualizedResult = fir2IrActualizedResult,
                outputKlibPath = klibFile.absolutePath,
                messageCollector = messageCollector,
                diagnosticsReporter = diagnosticsReporter,
                jsOutputName = moduleName
            )
        }

        if (messageCollector.hasErrors()) {
            val messages = outputStream.toByteArray().toString(Charset.forName("UTF-8"))
            throw AssertionError("The following errors occurred compiling test:\n$messages")
        }

    }

    private fun buildBinaryAndRun(mainModuleKlibFile: File, allDependencies: Dependencies) {
        val configuration = createConfig(MAIN_MODULE_NAME)
        configuration.setupPartialLinkageConfig(PartialLinkageConfig(PartialLinkageMode.ENABLE, PartialLinkageLogLevel.WARNING))

        val compilationOutputs = when (compilerType) {
            CompilerType.K1_NO_IC, CompilerType.K2_NO_IC -> buildBinaryNoIC(configuration, mainModuleKlibFile, allDependencies)
            CompilerType.K1_WITH_IC -> buildBinaryWithIC(configuration, mainModuleKlibFile, allDependencies)
        }

        val binariesDir = File(buildDir, BIN_DIR_NAME).also { it.mkdirs() }
        val binaries = compilationOutputs.writeAll(binariesDir, MAIN_MODULE_NAME, false, MAIN_MODULE_NAME, ModuleKind.PLAIN).filter {
            it.extension == "js"
        }

        executeAndCheckBinaries(MAIN_MODULE_NAME, binaries)
    }

    private fun buildBinaryNoIC(
        configuration: CompilerConfiguration,
        mainModuleKlibFile: File,
        allDependencies: Dependencies
    ): CompilationOutputs {
        val klib = MainModule.Klib(mainModuleKlibFile.path)
        val moduleStructure = ModulesStructure(
            environment.project,
            klib,
            configuration,
            allDependencies.regularDependencies.map { it.libraryFile.path },
            allDependencies.friendDependencies.map { it.libraryFile.path }
        )

        val ir = compile(
            moduleStructure,
            PhaseConfig(jsPhases),
            IrFactoryImplForJsIC(WholeWorldStageController()),
            exportedDeclarations = setOf(BOX_FUN_FQN),
            granularity = JsGenerationGranularity.PER_MODULE
        )

        val transformer = IrModuleToJsTransformer(
            backendContext = ir.context,
            mainArguments = emptyList()
        )

        val compiledResult = transformer.generateModule(
            modules = ir.allModules,
            modes = setOf(TranslationMode.PER_MODULE_DEV),
            relativeRequirePath = false
        )

        return compiledResult.outputs[TranslationMode.PER_MODULE_DEV] ?: error("No compiler output")
    }

    private fun buildBinaryWithIC(
        configuration: CompilerConfiguration,
        mainModuleKlibFile: File,
        allDependencies: Dependencies
    ): CompilationOutputs {
        // TODO: what about friend dependencies?
        val cacheUpdater = CacheUpdater(
            mainModule = mainModuleKlibFile.absolutePath,
            allModules = allDependencies.regularDependencies.map { it.libraryFile.path },
            mainModuleFriends = emptyList(),
            cacheDir = buildDir.resolve("libs-cache").absolutePath,
            compilerConfiguration = configuration,
            irFactory = { IrFactoryImplForJsIC(WholeWorldStageController()) },
            mainArguments = null,
            compilerInterfaceFactory = { mainModule, cfg ->
                JsIrCompilerWithIC(mainModule, cfg, JsGenerationGranularity.PER_MODULE, PhaseConfig(jsPhases), setOf(BOX_FUN_FQN))
            }
        )
        val icCaches = cacheUpdater.actualizeCaches()

        val mainModuleName = icCaches.last().moduleExternalName
        val jsExecutableProducer = JsExecutableProducer(
            mainModuleName = mainModuleName,
            moduleKind = configuration[JSConfigurationKeys.MODULE_KIND]!!,
            sourceMapsInfo = SourceMapsInfo.from(configuration),
            caches = icCaches,
            relativeRequirePath = true
        )

        return jsExecutableProducer.buildExecutable(multiModule = true, outJsProgram = true).compilationOut
    }

    private fun KotlinCoreEnvironment.createPsiFiles(sourceDir: File): List<KtFile> {
        val psiManager = PsiManager.getInstance(project)
        val fileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL) as CoreLocalFileSystem

        return sourceDir.walkTopDown().filter { file ->
            file.isFile && file.extension == "kt"
        }.flatMap { file ->
            val virtualFile = fileSystem.findFileByIoFile(file) ?: error("VirtualFile for $file not found")
            SingleRootFileViewProvider(psiManager, virtualFile).allFiles
        }.filterIsInstance<KtFile>().toList()
    }

    private fun File.binJsFile(name: String): File = File(this, "$name.js")

    private fun executeAndCheckBinaries(mainModuleName: String, dependencies: Collection<File>) {
        val checker = V8IrJsTestChecker

        val filePaths = dependencies.map { it.canonicalPath }
        checker.check(filePaths, mainModuleName, null, BOX_FUN_FQN.asString(), "OK", withModuleSystem = false)
    }

    companion object {
        private const val BIN_DIR_NAME = "_bins_js"
        private val BOX_FUN_FQN = FqName("box")
    }
}
