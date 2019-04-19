/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.benchmarks

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
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.junit.Test
import java.io.File

class GenerateIrRuntime {
    private val lookupTracker: LookupTracker = LookupTracker.DO_NOTHING
    private val logger = object : LoggingContext {
        override var inVerbosePhase = false
        override fun log(message: () -> String) {}
    }

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
                AnalysisFlags.useExperimental to listOf("kotlin.contracts.ExperimentalContracts", "kotlin.Experimental"),
                AnalysisFlags.allowResultReturnType to true
            )
        )

        return runtimeConfiguration
    }
    private val CompilerConfiguration.metadataVersion
        get() = get(CommonConfigurationKeys.METADATA_VERSION) as? JsKlibMetadataVersion ?: JsKlibMetadataVersion.INSTANCE


    private val environment =
        KotlinCoreEnvironment.createForTests(Disposable { }, CompilerConfiguration(), EnvironmentConfigFiles.JS_CONFIG_FILES)
    private val configuration = buildConfiguration(environment)
    private val project = environment.project
    private val phaseConfig = PhaseConfig(jsPhases)

    private val metadataVersion = configuration.metadataVersion
    private val languageVersionSettings = configuration.languageVersionSettings
    private val moduleName = configuration[CommonConfigurationKeys.MODULE_NAME]!!


    fun createPsiFile(fileName: String): KtFile {
        val psiManager = PsiManager.getInstance(environment.project)
        val fileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL)

        val file = fileSystem.findFileByPath(fileName) ?: error("File not found: $fileName")

        return psiManager.findFile(file) as KtFile
    }

    private fun File.listAllFiles(): List<File> {
        return if (isDirectory) listFiles().flatMap { it.listAllFiles() }
        else listOf(this)
    }

    private fun createPsiFileFromDir(path: String): List<KtFile> = File(path).listAllFiles().map { createPsiFile(it.path) }

    private val fullRuntimeSourceSet = createPsiFileFromDir("compiler/ir/serialization.js/build/fullRuntime/src")
    private val reducedRuntimeSourceSet = createPsiFileFromDir("compiler/ir/serialization.js/build/reducedRuntime/src")

    @Test
    fun runFullPipeline() {
        repeat(1) {
            compile(fullRuntimeSourceSet)
        }
    }

    @Test
    fun runWithoutFrontEnd() {
        val files = fullRuntimeSourceSet
        val analysisResult = doFrontEnd(files)

        repeat(10) {
            val rawModuleFragment = doPsi2Ir(files, analysisResult)

            val moduleRef = doSerializeModule(rawModuleFragment, analysisResult.bindingContext)

            val (module, symbolTable, irBuiltIns) = doDeserializeModule(moduleRef)

            val jsProgram = doBackEnd(module, symbolTable, irBuiltIns)
        }
    }

    @Test
    fun runPsi2Ir() {
        val files = reducedRuntimeSourceSet
        val analysisResult = doFrontEnd(files)

        repeat(200) {
            doPsi2Ir(files, analysisResult)
        }
    }

    @Test
    fun runSerialization() {
        val files = reducedRuntimeSourceSet
        val analysisResult = doFrontEnd(files)
        val rawModuleFragment = doPsi2Ir(files, analysisResult)

        repeat(20) {
            doSerializeModule(rawModuleFragment, analysisResult.bindingContext)
        }
    }

    @Test
    fun runDeserialization() {
        val files = reducedRuntimeSourceSet
        val analysisResult = doFrontEnd(files)
        val rawModuleFragment = doPsi2Ir(files, analysisResult)
        val moduleRef = doSerializeModule(rawModuleFragment, analysisResult.bindingContext)

        repeat(20) {
            doDeserializeModule(moduleRef)
        }
    }

    @Test
    fun runDeserializationAndBackend() {
        val files = reducedRuntimeSourceSet
        val analysisResult = doFrontEnd(files)
        val rawModuleFragment = doPsi2Ir(files, analysisResult)
        val moduleRef = doSerializeModule(rawModuleFragment, analysisResult.bindingContext)

        repeat(100) {
            val (module, symbolTable, irBuiltIns) = doDeserializeModule(moduleRef)
            doBackEnd(module, symbolTable, irBuiltIns)
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

    private data class DeserializedModuleInfo(val module: IrModuleFragment, val symbolTable: SymbolTable, val irBuiltIns: IrBuiltIns)

    private fun doDeserializeModule(moduleRef: KlibModuleRef): DeserializedModuleInfo {
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

        return DeserializedModuleInfo(moduleFragment, symbolTable, irBuiltIns)
    }

    private fun doBackEnd(module: IrModuleFragment, symbolTable: SymbolTable, irBuiltIns: IrBuiltIns): JsProgram {
        val context = JsIrBackendContext(module.descriptor, irBuiltIns, symbolTable, module, configuration)

        ExternalDependenciesGenerator(module.descriptor, symbolTable, irBuiltIns).generateUnboundSymbolsAsDependencies()

        jsPhases.invokeToplevel(phaseConfig, context, module)

        return module.accept(IrModuleToJsTransformer(context), null) as JsProgram
    }

    fun compile(files: List<KtFile>): String {
        val analysisResult = doFrontEnd(files)

        val rawModuleFragment = doPsi2Ir(files, analysisResult)

        val moduleRef = doSerializeModule(rawModuleFragment, analysisResult.bindingContext)

        val (module, symbolTable, irBuiltIns) = doDeserializeModule(moduleRef)

        val jsProgram = doBackEnd(module, symbolTable, irBuiltIns)

        return jsProgram.toString()
    }
}