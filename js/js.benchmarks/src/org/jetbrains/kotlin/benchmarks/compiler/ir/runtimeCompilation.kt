/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.benchmarks.compiler.ir

import com.intellij.openapi.Disposable
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.backend.common.LoggingContext
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.common.phaser.invokeToplevel
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.ir.backend.js.*
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsIrLinker
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.metadata.JsKlibMetadataVersion
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.IrModuleToJsTransformer
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.js.analyze.TopDownAnalyzerFacadeForJS
import org.jetbrains.kotlin.js.backend.ast.JsProgram
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi2ir.Psi2IrConfiguration
import org.jetbrains.kotlin.psi2ir.Psi2IrTranslator
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext
import org.jetbrains.kotlin.psi2ir.generators.GeneratorExtensions
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.multiplatform.isCommonSource
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole
import java.io.File
import java.util.concurrent.TimeUnit

@State(Scope.Benchmark)
@Fork(1)
@BenchmarkMode(Mode.AverageTime)
@OperationsPerInvocation(1)
open class GenerateIrRuntime {

    companion object {

        private val lookupTracker: LookupTracker = LookupTracker.DO_NOTHING

        private val logger = object : LoggingContext {
            override var inVerbosePhase = false
            override fun log(message: () -> String) {}
        }

        private fun buildConfiguration(runtimeConfiguration: CompilerConfiguration): CompilerConfiguration {
            runtimeConfiguration.put(CommonConfigurationKeys.MODULE_NAME, "JS_IR_RUNTIME")
            runtimeConfiguration.put(JSConfigurationKeys.MODULE_KIND, ModuleKind.UMD)

            runtimeConfiguration.languageVersionSettings = LanguageVersionSettingsImpl(
                LanguageVersion.LATEST_STABLE, ApiVersion.LATEST_STABLE,
                specificFeatures = mapOf(
                    LanguageFeature.AllowContractsForCustomFunctions to LanguageFeature.State.ENABLED,
                    LanguageFeature.MultiPlatformProjects to LanguageFeature.State.ENABLED
                ),
                analysisFlags = mapOf(
                    AnalysisFlags.useExperimental to listOf(
                        "kotlin.contracts.ExperimentalContracts",
                        "kotlin.Experimental",
                        "kotlin.ExperimentalMultiplatform"
                    ),
                    AnalysisFlags.allowResultReturnType to true
                )
            )

            return runtimeConfiguration
        }


        private fun metadataVersion(config: CompilerConfiguration) =
            config[CommonConfigurationKeys.METADATA_VERSION] as? JsKlibMetadataVersion ?: JsKlibMetadataVersion.INSTANCE


        private val environment =
            KotlinCoreEnvironment.createForTests(Disposable {  }, CompilerConfiguration(), EnvironmentConfigFiles.JS_CONFIG_FILES)
        private val configuration = buildConfiguration(environment.configuration)
        private val project = environment.project
        private val phaseConfig = PhaseConfig(jsPhases)

        private val metadataVersion = metadataVersion(configuration)
        private val languageVersionSettings = configuration.languageVersionSettings
        private val moduleName = configuration[CommonConfigurationKeys.MODULE_NAME]!!

        private val commonSourceDirectories = listOf("common", "src", "unsigned").map {
            "fullRuntime/src/libraries/stdlib/$it"
        }

        private fun isCommonSource(path: String) = commonSourceDirectories.any { path.contains(it) }

        private fun createPsiFile(fileName: String): KtFile {
            val psiManager = PsiManager.getInstance(project)
            val fileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL)

            val file = fileSystem.findFileByPath(fileName) ?: error("File not found: $fileName")

            return (psiManager.findFile(file) as KtFile).apply { isCommonSource = isCommonSource(fileName) }
        }

        private fun File.listAllFiles(): List<File> {
            return if (isDirectory) listFiles().flatMap { it.listAllFiles() }
            else listOf(this)
        }

        private fun createPsiFileFromDir(path: String): List<KtFile> = File(path).listAllFiles().map { createPsiFile(it.path) }

        private val fullRuntimeSourceSet = createPsiFileFromDir("compiler/ir/serialization.js/build/fullRuntime/src")
    }


    private val sharedSourceSet = fullRuntimeSourceSet
    private var sharedAnalysisResult: AnalysisResult? = null
    private var sharedModuleFragment: IrModuleFragment? = null
    private var sharedModuleRef: KlibModuleRef? = null

    @Setup
    fun setupSharedData() {
        val files = sharedSourceSet
        val analysisResult = doFrontEnd(files).also { sharedAnalysisResult = it }
        val moduleFragment = doPsi2Ir(files, analysisResult).also { sharedModuleFragment = it }
        sharedModuleRef = doSerializeModule(moduleFragment, analysisResult.bindingContext)
    }


    @Benchmark
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Warmup(iterations = 20, batchSize = 1)
    @Measurement(iterations = 5, batchSize = 1)
    fun runFullPipeline(bh: Blackhole) {
        bh.consume(compile(sharedSourceSet))
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Warmup(iterations = 40, batchSize = 1)
    @Measurement(iterations = 10, batchSize = 1)
    fun runWithoutFrontEnd(bh: Blackhole) {
        val files = sharedSourceSet
        val analysisResult = sharedAnalysisResult!!

        val rawModuleFragment = doPsi2Ir(files, analysisResult)
        val moduleRef = doSerializeModule(rawModuleFragment, analysisResult.bindingContext)
        val (module, symbolTable, irBuiltIns) = doDeserializeModule(moduleRef)
        bh.consume(doBackEnd(module, symbolTable, irBuiltIns))
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @Warmup(iterations = 100, time = 2, timeUnit = TimeUnit.SECONDS, batchSize = 1)
    @Measurement(iterations = 20, time = 2, timeUnit = TimeUnit.SECONDS, batchSize = 1)
    fun runPsi2Ir(bh: Blackhole) {
        val files = sharedSourceSet
        val analysisResult = sharedAnalysisResult!!

        bh.consume(doPsi2Ir(files, analysisResult))
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @Warmup(iterations = 100, time = 2, timeUnit = TimeUnit.SECONDS, batchSize = 1)
    @Measurement(iterations = 20, time = 2, timeUnit = TimeUnit.SECONDS, batchSize = 1)
    fun runSerialization(bh: Blackhole) {
        val analysisResult = sharedAnalysisResult!!
        val rawModuleFragment = sharedModuleFragment!!

        bh.consume(doSerializeModule(rawModuleFragment, analysisResult.bindingContext))
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @Warmup(iterations = 50, batchSize = 1)
    @Measurement(iterations = 10, batchSize = 1)
    fun runDeserialization(bh: Blackhole) {
        val moduleRef = sharedModuleRef!!

        bh.consume(doDeserializeModule(moduleRef))
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Warmup(iterations = 50, batchSize = 1)
    @Measurement(iterations = 10, batchSize = 1)
    fun runDeserializationAndBackend(bh: Blackhole) {
        val moduleRef = sharedModuleRef!!

        val (module, symbolTable, irBuiltIns) = doDeserializeModule(moduleRef)
        bh.consume(doBackEnd(module, symbolTable, irBuiltIns))
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
        TopDownAnalyzerFacadeForJS.checkForErrors(files, analysisResult.bindingContext)

        return analysisResult
    }

    private fun doPsi2Ir(files: List<KtFile>, analysisResult: AnalysisResult): IrModuleFragment {
        val psi2IrContext = GeneratorContext(
            Psi2IrConfiguration(),
            analysisResult.moduleDescriptor,
            analysisResult.bindingContext,
            languageVersionSettings,
            SymbolTable(),
            GeneratorExtensions()
        )
        return Psi2IrTranslator(languageVersionSettings, psi2IrContext.configuration).generateModuleFragment(psi2IrContext, files)
    }

    private fun doSerializeModule(moduleFragment: IrModuleFragment, bindingContext: BindingContext): KlibModuleRef {
        val tmpKlibDir = createTempDir().also { it.deleteOnExit() }
        serializeModuleIntoKlib(
            moduleName,
            metadataVersion,
            languageVersionSettings,
            bindingContext,
            tmpKlibDir.path,
            emptyList(),
            moduleFragment
        )

        return KlibModuleRef(moduleName, tmpKlibDir.path)
    }

    private fun doDeserializeModuleMetadata(moduleRef: KlibModuleRef): ModuleDescriptorImpl {
        val storageManager = LockBasedStorageManager("ModulesStructure")

        val parts = loadKlibMetadataParts(moduleRef)

        return loadKlibMetadata(
            parts,
            moduleRef,
            true,
            lookupTracker,
            storageManager,
            metadataVersion,
            languageVersionSettings,
            null,
            emptyList()
        )
    }

    private fun doDeserializeModule(moduleRef: KlibModuleRef): Triple<IrModuleFragment, SymbolTable, IrBuiltIns> {
        val moduleDescriptor = doDeserializeModuleMetadata(moduleRef)

        val symbolTable = SymbolTable()
        val typeTranslator = TypeTranslator(symbolTable, languageVersionSettings, moduleDescriptor.builtIns).also {
            it.constantValueGenerator = ConstantValueGenerator(moduleDescriptor, symbolTable)
        }

        val irBuiltIns = IrBuiltIns(moduleDescriptor.builtIns, typeTranslator, symbolTable)

        val jsLinker = JsIrLinker(moduleDescriptor, logger, irBuiltIns, symbolTable)

        val moduleFragment = jsLinker.deserializeFullModule(moduleDescriptor)
        // Create stubs
        ExternalDependenciesGenerator(
            moduleDescriptor = moduleDescriptor,
            symbolTable = symbolTable,
            irBuiltIns = irBuiltIns,
            deserializer = jsLinker
        ).generateUnboundSymbolsAsDependencies()

        moduleFragment.patchDeclarationParents()

        return Triple(moduleFragment, symbolTable, irBuiltIns)
    }

    private fun doBackEnd(module: IrModuleFragment, symbolTable: SymbolTable, irBuiltIns: IrBuiltIns): JsProgram {
        val context = JsIrBackendContext(module.descriptor, irBuiltIns, symbolTable, module, configuration)

        ExternalDependenciesGenerator(module.descriptor, symbolTable, irBuiltIns).generateUnboundSymbolsAsDependencies()

        jsPhases.invokeToplevel(phaseConfig, context, module)

        return module.accept(IrModuleToJsTransformer(context), null) as JsProgram
    }

    private fun compile(files: List<KtFile>): String {
        val analysisResult = doFrontEnd(files)

        val rawModuleFragment = doPsi2Ir(files, analysisResult)

        val moduleRef = doSerializeModule(rawModuleFragment, analysisResult.bindingContext)

        val (module, symbolTable, irBuiltIns) = doDeserializeModule(moduleRef)

        val jsProgram = doBackEnd(module, symbolTable, irBuiltIns)

        return jsProgram.toString()
    }
}