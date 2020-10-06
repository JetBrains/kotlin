/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.benchmarks

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.backend.common.LoggingContext
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.common.phaser.invokeToplevel
import org.jetbrains.kotlin.backend.common.serialization.KlibIrVersion
import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibMetadataVersion
import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureDescriptor
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.descriptors.konan.kotlinLibrary
import org.jetbrains.kotlin.incremental.ChangedFiles
import org.jetbrains.kotlin.incremental.EmptyICReporter
import org.jetbrains.kotlin.incremental.IncrementalJsCompilerRunner
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.multiproject.EmptyModulesApiHistory
import org.jetbrains.kotlin.incremental.withJsIC
import org.jetbrains.kotlin.ir.backend.js.*
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsIrLinker
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsIrModuleSerializer
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsManglerDesc
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.IrModuleToJsTransformer
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.persistent.PersistentIrFactory
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.descriptors.IrFunctionFactory
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.js.analyze.TopDownAnalyzerFacadeForJS
import org.jetbrains.kotlin.js.config.ErrorTolerancePolicy
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.KotlinLibraryVersioning
import org.jetbrains.kotlin.library.SerializedIrModule
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.library.impl.KotlinLibraryOnlyIrWriter
import org.jetbrains.kotlin.library.impl.createKotlinLibrary
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi2ir.Psi2IrConfiguration
import org.jetbrains.kotlin.psi2ir.Psi2IrTranslator
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.multiplatform.isCommonSource
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import java.io.File
import org.jetbrains.kotlin.konan.file.File as KonanFile

@Ignore
class GenerateIrRuntime {
    private val lookupTracker: LookupTracker = LookupTracker.DO_NOTHING
    private val logger = object : LoggingContext {
        override var inVerbosePhase = false
        override fun log(message: () -> String) {}
    }

    fun loadKlib(klibPath: String, isPacked: Boolean) = createKotlinLibrary(KonanFile("$klibPath${if (isPacked) ".klib" else ""}"))

    private fun buildConfiguration(environment: KotlinCoreEnvironment): CompilerConfiguration {
        val runtimeConfiguration = environment.configuration.copy()
        runtimeConfiguration.put(CommonConfigurationKeys.MODULE_NAME, "JS_IR_RUNTIME")
        runtimeConfiguration.put(JSConfigurationKeys.MODULE_KIND, ModuleKind.UMD)

        runtimeConfiguration.languageVersionSettings = LanguageVersionSettingsImpl(
            LanguageVersion.LATEST_STABLE, ApiVersion.LATEST_STABLE,
            specificFeatures = mapOf(
                LanguageFeature.AllowContractsForCustomFunctions to LanguageFeature.State.ENABLED,
                LanguageFeature.MultiPlatformProjects to LanguageFeature.State.ENABLED
            ),
            analysisFlags = mapOf(
                AnalysisFlags.useExperimental to listOf("kotlin.contracts.ExperimentalContracts", "kotlin.Experimental", "kotlin.ExperimentalMultiplatform"),
                AnalysisFlags.allowResultReturnType to true
            )
        )

        return runtimeConfiguration
    }
    private val CompilerConfiguration.metadataVersion
        get() = get(CommonConfigurationKeys.METADATA_VERSION) as? KlibMetadataVersion ?: KlibMetadataVersion.INSTANCE

    private val environment =
        KotlinCoreEnvironment.createForTests(Disposable { }, CompilerConfiguration(), EnvironmentConfigFiles.JS_CONFIG_FILES)
    private val configuration = buildConfiguration(environment)
    private val project = environment.project
    private val phaseConfig = PhaseConfig(jsPhases)

    private val metadataVersion = configuration.metadataVersion
    private val languageVersionSettings = configuration.languageVersionSettings
    private val moduleName = configuration[CommonConfigurationKeys.MODULE_NAME]!!

    fun createPsiFile(fileName: String, isCommon: Boolean): KtFile? {
        val psiManager = PsiManager.getInstance(environment.project)
        val fileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL)

        val file = fileSystem.findFileByPath(fileName) ?: error("File not found: $fileName")

        val psiFile = psiManager.findFile(file)

        return (psiFile as? KtFile)?.apply { isCommonSource = isCommon }
    }

    private fun File.listAllFiles(): List<File> {
        return if (isDirectory) listFiles().flatMap { it.listAllFiles() }
        else listOf(this)
    }

    private fun createPsiFileFromDir(path: String, vararg extraDirs: String): List<KtFile> {
        val dir = File(path)
        val buildPath = File(dir, "build")
        val commonPath = File(buildPath, "commonMainSources")
        val extraPaths = extraDirs.map { File(dir, it) }
        val jsPaths = listOf(File(buildPath, "jsMainSources")) + extraPaths
        val commonPsis = commonPath.listAllFiles().mapNotNull { createPsiFile(it.path, true) }
        val jsPsis = jsPaths.flatMap { d -> d.listAllFiles().mapNotNull { createPsiFile(it.path, false) } }
        return commonPsis + jsPsis
    }

    private val fullRuntimeSourceSet = createPsiFileFromDir("libraries/stdlib/js-ir", "builtins", "runtime", "src")
    private val reducedRuntimeSourceSet = createPsiFileFromDir("libraries/stdlib/js-ir-minimal-for-test", "src")

    private lateinit var workingDir: File

    @Before
    fun setUp() {
        workingDir = FileUtil.createTempDirectory("irTest", null, false)
    }

    @After
    fun tearDown() {
        workingDir.deleteRecursively()
    }

    @Test
    fun runFullPipeline() {
        runBenchWithWarmup("Full pipeline", 5, 2, MeasureUnits.MICROSECONDS, pre = System::gc) {
            compile(fullRuntimeSourceSet)
        }
    }

    @Test
    fun runWithoutFrontEnd() {
        val files = fullRuntimeSourceSet
        val analysisResult = doFrontEnd(files)

        runBenchWithWarmup("Pipeline withput FrontEnd", 40, 10, MeasureUnits.MICROSECONDS, pre = System::gc) {
            val rawModuleFragment = doPsi2Ir(files, analysisResult)

            val modulePath = doSerializeModule(rawModuleFragment, analysisResult.bindingContext, files)

            val (module, symbolTable, irBuiltIns, linker) = doDeserializeModule(modulePath)

            val jsProgram = doBackEnd(module, symbolTable, irBuiltIns, linker)
        }
    }

    @Test
    fun runPsi2Ir() {
        val files = fullRuntimeSourceSet
        val analysisResult = doFrontEnd(files)

        runBenchWithWarmup("Psi2Ir phase", 40, 10, MeasureUnits.MICROSECONDS, pre = System::gc) {
            doPsi2Ir(files, analysisResult)
        }
    }

    @Test
    fun runSerialization() {
        val files = fullRuntimeSourceSet
        val analysisResult = doFrontEnd(files)
        val rawModuleFragment = doPsi2Ir(files, analysisResult)

        runBenchWithWarmup("Ir Serialization", 40, 10, MeasureUnits.MILLISECONDS, pre = System::gc) {
            doSerializeModule(rawModuleFragment, analysisResult.bindingContext, files)
        }
    }

    enum class MeasureUnits(val delimeter: Long, private val suffix: String) {
        NANOSECONDS(1L, "ns"),
        MICROSECONDS(1000L, "mcs"),
        MILLISECONDS(1000L * 1000L, "ms");

        fun convert(nanos: Long): String = "${(nanos / delimeter)}$suffix"
    }

    @Test
    fun runIrDeserializationMonolitic() {
        val files = fullRuntimeSourceSet
        val analysisResult = doFrontEnd(files)
        val rawModuleFragment = doPsi2Ir(files, analysisResult)
        val modulePath = doSerializeModule(rawModuleFragment, analysisResult.bindingContext, files, false)
        val moduleRef = loadKlib(modulePath, isPacked = false)
        val moduleDescriptor = doDeserializeModuleMetadata(moduleRef)

        runBenchWithWarmup("Ir Deserialization Monolithic", 40, 10, MeasureUnits.MILLISECONDS, pre = System::gc) {
            doDeserializeIrModule(moduleDescriptor)
        }
    }

    @Test
    fun runIrDeserializationPerFile() {
        val files = fullRuntimeSourceSet
        val analysisResult = doFrontEnd(files)
        val rawModuleFragment = doPsi2Ir(files, analysisResult)
        val modulePath = doSerializeModule(rawModuleFragment, analysisResult.bindingContext, files, true)
        val moduleRef = loadKlib(modulePath, isPacked = false)
        val moduleDescriptor = doDeserializeModuleMetadata(moduleRef)

        runBenchWithWarmup("Ir Deserialization Per-File", 40, 10, MeasureUnits.MILLISECONDS, pre = System::gc) {
            doDeserializeIrModule(moduleDescriptor)
        }
    }

    @Test
    fun runIrSerialization() {
        val files = fullRuntimeSourceSet
        val analysisResult = doFrontEnd(files)
        val rawModuleFragment = doPsi2Ir(files, analysisResult)

        runBenchWithWarmup("Ir Serialization", 40, 10, MeasureUnits.MILLISECONDS, pre = System::gc) {
            doSerializeIrModule(rawModuleFragment)
        }
    }

    @Test
    fun runMonoliticDiskWriting() {
        val libraryVersion = "JSIR"
        val compilerVersion = KotlinCompilerVersion.getVersion()
        val abiVersion = KotlinAbiVersion.CURRENT
        val metadataVersion = KlibMetadataVersion.INSTANCE.toString()
        val irVersion = KlibIrVersion.INSTANCE.toString()

        val versions = KotlinLibraryVersioning(libraryVersion, compilerVersion, abiVersion, metadataVersion, irVersion)
        val file = createTempFile(directory = workingDir)
        val writer = KotlinLibraryOnlyIrWriter(file.absolutePath, "", versions, BuiltInsPlatform.JS, emptyList(), false)
        val files = fullRuntimeSourceSet
        val analysisResult = doFrontEnd(files)
        val rawModuleFragment = doPsi2Ir(files, analysisResult)
        val fileCount = rawModuleFragment.files.size
        val serializedIr = doSerializeIrModule(rawModuleFragment)

        runBenchWithWarmup("Monolitic Disk Writing of $fileCount files", 10, 30, MeasureUnits.MILLISECONDS, pre = writer::invalidate) {
            doWriteIrModuleToStorage(serializedIr, writer)
        }
    }

    @Test
    fun runPerfileDiskWriting() {
        val libraryVersion = "JSIR"
        val compilerVersion = KotlinCompilerVersion.getVersion()
        val abiVersion = KotlinAbiVersion.CURRENT
        val metadataVersion = KlibMetadataVersion.INSTANCE.toString()
        val irVersion = KlibIrVersion.INSTANCE.toString()

        val versions = KotlinLibraryVersioning(libraryVersion, compilerVersion, abiVersion, metadataVersion, irVersion)
        val file = createTempFile(directory = workingDir)
        val writer = KotlinLibraryOnlyIrWriter(file.absolutePath, "", versions, BuiltInsPlatform.JS, emptyList(), true)
        val files = fullRuntimeSourceSet
        val analysisResult = doFrontEnd(files)
        val rawModuleFragment = doPsi2Ir(files, analysisResult)
        val fileCount = rawModuleFragment.files.size
        val serializedIr = doSerializeIrModule(rawModuleFragment)

        runBenchWithWarmup("Per-file Disk Writing of $fileCount files", 10, 30, MeasureUnits.MILLISECONDS, pre = writer::invalidate) {
            doWriteIrModuleToStorage(serializedIr, writer)
        }
    }

    @Test
    fun runIncrementalKlibGeneratation() {

        val klibDirectory = workingDir.resolve("output/klib")

        val filesToCompile = fullRuntimeSourceSet
//        val filesToCompile = reducedRuntimeSourceSet

        val args = K2JSCompilerArguments().apply {
            libraries = ""
            outputFile = klibDirectory.path
            sourceMap = false
            irProduceKlibDir = true
            irOnly = true
            irModuleName = "kotlin"
            allowKotlinPackage = true
            useExperimental = arrayOf("kotlin.contracts.ExperimentalContracts", "kotlin.Experimental", "kotlin.ExperimentalMultiplatform")
            allowResultReturnType = true
            multiPlatform = true
            languageVersion = "1.4"
            commonSources = filesToCompile.filter { it.isCommonSource == true }.map { it.virtualFilePath }.toTypedArray()
        }

        val cachesDir = workingDir.resolve("caches")
        val allFiles = filesToCompile.map { VfsUtilCore.virtualToIoFile(it.virtualFile) }
        val dirtyFiles = allFiles.filter { it.name.contains("coreRuntime") }

        val cleanBuildStart = System.nanoTime()

        withJsIC {
            val buildHistoryFile = File(cachesDir, "build-history.bin")
            val compiler = IncrementalJsCompilerRunner(
                cachesDir, EmptyICReporter,
                buildHistoryFile = buildHistoryFile,
                modulesApiHistory = EmptyModulesApiHistory
            )
            compiler.compile(allFiles, args, MessageCollector.NONE, providedChangedFiles = null)
        }

        val cleanBuildTime = System.nanoTime() - cleanBuildStart

        println("[Cold] Clean build of ${allFiles.size} takes ${MeasureUnits.MILLISECONDS.convert(cleanBuildTime)}")

        var index = -1
        val wmpDone = { index = 0 }

        val elist = emptyList<File>()
        var changedFiles = ChangedFiles.Known(dirtyFiles, elist)

        val update = {
            changedFiles = if (index < 0) changedFiles else ChangedFiles.Known(listOf(allFiles[index++]), elist)
            System.gc()
        }

        class CompileTimeResult(val file: String, val time: Long)

        var maxResult = CompileTimeResult("", -1)
        var minResult = CompileTimeResult("", Long.MAX_VALUE)

        val done = { t: Long ->
            if (maxResult.time < t) maxResult = CompileTimeResult(changedFiles.modified[0].path, t)
            if (minResult.time > t) minResult = CompileTimeResult(changedFiles.modified[0].path, t)
        }

        runBenchWithWarmup(
            "Incremental recompilation of ${dirtyFiles.count()} files",
            200,
            allFiles.size,
            MeasureUnits.MILLISECONDS,
            wmpDone,
            done,
            update
        ) {
            withJsIC {
                val buildHistoryFile = File(cachesDir, "build-history.bin")
                val compiler = IncrementalJsCompilerRunner(
                    cachesDir, EmptyICReporter,
                    buildHistoryFile = buildHistoryFile,
                    modulesApiHistory = EmptyModulesApiHistory
                )
                compiler.compile(allFiles, args, MessageCollector.NONE, changedFiles)
            }
        }

        println("Longest re-compilation takes ${MeasureUnits.MILLISECONDS.convert(maxResult.time)} (${maxResult.file})")
        println("Fastest re-compilation takes ${MeasureUnits.MILLISECONDS.convert(minResult.time)} (${minResult.file})")
    }

    private fun runBenchWithWarmup(name: String, W: Int, N: Int, measurer: MeasureUnits, wmpDone: () -> Unit = {}, bnhDone: (Long) -> Unit = {}, pre: () -> Unit = {}, bench: () -> Unit) {

        println("Run $name benchmark")

        println("Warmup: $W times...")

        repeat(W) {
            println("W: ${it + 1} out of $W")
            pre()
            bench()
        }

        var total = 0L

        wmpDone()

        println("Run bench: $N times...")

        repeat(N) {
            print("R: ${it + 1} out of $N ")
            pre()
            val start = System.nanoTime()
            bench()
            val iter = System.nanoTime() - start
            println("takes ${measurer.convert(iter)}")
            bnhDone(iter)
            total += iter
        }

        println("$name takes ${measurer.convert(total / N)}")
    }

    @Test
    fun runDeserialization() {
        val files = fullRuntimeSourceSet
        val analysisResult = doFrontEnd(files)
        val rawModuleFragment = doPsi2Ir(files, analysisResult)
        val modulePath = doSerializeModule(rawModuleFragment, analysisResult.bindingContext, files)

        repeat(20) {
            doDeserializeModule(modulePath)
        }
    }

    @Test
    fun runDeserializationAndBackend() {
        val files = fullRuntimeSourceSet
        val analysisResult = doFrontEnd(files)
        val rawModuleFragment = doPsi2Ir(files, analysisResult)
        val modulePath = doSerializeModule(rawModuleFragment, analysisResult.bindingContext, files)

        runBenchWithWarmup("Deserializtion and Backend", 40, 10, MeasureUnits.MICROSECONDS, pre = System::gc) {
            val (module, symbolTable, irBuiltIns, linker) = doDeserializeModule(modulePath)
            doBackEnd(module, symbolTable, irBuiltIns, linker)
        }
    }

    private fun doFrontEnd(files: List<KtFile>): AnalysisResult {
        val analysisResult =
            TopDownAnalyzerFacadeForJS.analyzeFiles(
                files,
                project,
                configuration,
                emptyList(),
                friendModuleDescriptors = emptyList(),
                thisIsBuiltInsModule = true,
                customBuiltInsModule = null
            )

        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()
        TopDownAnalyzerFacadeForJS.checkForErrors(files, analysisResult.bindingContext, ErrorTolerancePolicy.NONE)

        return analysisResult
    }

    private fun doPsi2Ir(files: List<KtFile>, analysisResult: AnalysisResult): IrModuleFragment {
        val psi2Ir = Psi2IrTranslator(languageVersionSettings, Psi2IrConfiguration())
        val symbolTable = SymbolTable(IdSignatureDescriptor(JsManglerDesc), PersistentIrFactory)
        val psi2IrContext = psi2Ir.createGeneratorContext(analysisResult.moduleDescriptor, analysisResult.bindingContext, symbolTable)

        val irBuiltIns = psi2IrContext.irBuiltIns
        val functionFactory = IrFunctionFactory(irBuiltIns, psi2IrContext.symbolTable)
        irBuiltIns.functionFactory = functionFactory

        val irLinker = JsIrLinker(
            psi2IrContext.moduleDescriptor,
            emptyLoggingContext,
            psi2IrContext.irBuiltIns,
            psi2IrContext.symbolTable,
            functionFactory,
            null
        )

        val irProviders = listOf(irLinker)

        val psi2IrTranslator = Psi2IrTranslator(languageVersionSettings, psi2IrContext.configuration)
        return psi2IrTranslator.generateModuleFragment(psi2IrContext, files, irProviders, emptyList(), null)
    }

    private fun doSerializeModule(moduleFragment: IrModuleFragment, bindingContext: BindingContext, files: List<KtFile>, perFile: Boolean = false): String {
        val tmpKlibDir = createTempDir().also { it.deleteOnExit() }
        serializeModuleIntoKlib(
            moduleName,
            configuration,
            bindingContext,
            files,
            tmpKlibDir.path,
            emptyList(),
            moduleFragment,
            mutableMapOf(),
            emptyList(),
            true,
            perFile
        )

        return tmpKlibDir.path
    }

    private fun doDeserializeModuleMetadata(moduleRef: KotlinLibrary): ModuleDescriptorImpl {
        return getModuleDescriptorByLibrary(moduleRef, emptyMap())
    }

    private data class DeserializedModuleInfo(val module: IrModuleFragment, val symbolTable: SymbolTable, val irBuiltIns: IrBuiltIns, val linker: JsIrLinker)


    private fun doSerializeIrModule(module: IrModuleFragment): SerializedIrModule {
        val serializedIr = JsIrModuleSerializer(logger, module.irBuiltins, mutableMapOf(), true).serializedIrModule(module)
        return serializedIr
    }

    private fun doWriteIrModuleToStorage(serializedIrModule: SerializedIrModule, writer: KotlinLibraryOnlyIrWriter) {
        writer.writeIr(serializedIrModule)
    }

    private fun doDeserializeIrModule(moduleDescriptor: ModuleDescriptorImpl): DeserializedModuleInfo {
        val mangler = JsManglerDesc
        val signaturer = IdSignatureDescriptor(mangler)
        val symbolTable = SymbolTable(signaturer, PersistentIrFactory)
        val typeTranslator = TypeTranslator(symbolTable, languageVersionSettings, moduleDescriptor.builtIns).also {
            it.constantValueGenerator = ConstantValueGenerator(moduleDescriptor, symbolTable)
        }

        val irBuiltIns = IrBuiltIns(moduleDescriptor.builtIns, typeTranslator, symbolTable)
        val functionFactory = IrFunctionFactory(irBuiltIns, symbolTable)
        irBuiltIns.functionFactory = functionFactory

        val jsLinker = JsIrLinker(moduleDescriptor, logger, irBuiltIns, symbolTable, functionFactory, null)

        val moduleFragment = jsLinker.deserializeFullModule(moduleDescriptor, moduleDescriptor.kotlinLibrary)
        jsLinker.init(null, emptyList())
        // Create stubs
        ExternalDependenciesGenerator(symbolTable, listOf(jsLinker), languageVersionSettings)
            .generateUnboundSymbolsAsDependencies()

        jsLinker.postProcess()

        moduleFragment.patchDeclarationParents()

        return DeserializedModuleInfo(moduleFragment, symbolTable, irBuiltIns, jsLinker)
    }

    private fun doDeserializeModule(modulePath: String): DeserializedModuleInfo {
        val moduleRef = loadKlib(modulePath, false)
        val moduleDescriptor = doDeserializeModuleMetadata(moduleRef)
        val mangler = JsManglerDesc
        val signaturer = IdSignatureDescriptor(mangler)
        val symbolTable = SymbolTable(signaturer, PersistentIrFactory)
        val typeTranslator = TypeTranslator(symbolTable, languageVersionSettings, moduleDescriptor.builtIns).also {
            it.constantValueGenerator = ConstantValueGenerator(moduleDescriptor, symbolTable)
        }

        val irBuiltIns = IrBuiltIns(moduleDescriptor.builtIns, typeTranslator, symbolTable)

        val functionFactory = IrFunctionFactory(irBuiltIns, symbolTable)
        irBuiltIns.functionFactory = functionFactory

        val jsLinker = JsIrLinker(moduleDescriptor, logger, irBuiltIns, symbolTable, functionFactory, null)

        val moduleFragment = jsLinker.deserializeFullModule(moduleDescriptor, moduleDescriptor.kotlinLibrary)
        // Create stubs
        jsLinker.init(null, emptyList())
        // Create stubs
        ExternalDependenciesGenerator(symbolTable, listOf(jsLinker), languageVersionSettings)
            .generateUnboundSymbolsAsDependencies()

        jsLinker.postProcess()

        moduleFragment.patchDeclarationParents()

        return DeserializedModuleInfo(moduleFragment, symbolTable, irBuiltIns, jsLinker)
    }


    private fun doBackEnd(module: IrModuleFragment, symbolTable: SymbolTable, irBuiltIns: IrBuiltIns, jsLinker: JsIrLinker): CompilerResult {
        val context = JsIrBackendContext(module.descriptor, irBuiltIns, symbolTable, module, emptySet(), configuration)

        ExternalDependenciesGenerator(symbolTable, listOf(jsLinker), languageVersionSettings).generateUnboundSymbolsAsDependencies()

        jsPhases.invokeToplevel(phaseConfig, context, listOf(module))

        val transformer = IrModuleToJsTransformer(context, null)

        return transformer.generateModule(listOf(module))
    }

    fun compile(files: List<KtFile>): String {
        val analysisResult = doFrontEnd(files)

        val rawModuleFragment = doPsi2Ir(files, analysisResult)

        val modulePath = doSerializeModule(rawModuleFragment, analysisResult.bindingContext, files)

        val (module, symbolTable, irBuiltIns, linker) = doDeserializeModule(modulePath)

        val jsProgram = doBackEnd(module, symbolTable, irBuiltIns, linker)

        return jsProgram.toString()
    }
}