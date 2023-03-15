package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.serialization.metadata.DynamicTypeDeserializer
import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.backend.konan.driver.phases.Fir2IrOutput
import org.jetbrains.kotlin.backend.konan.driver.phases.FirOutput
import org.jetbrains.kotlin.backend.konan.ir.KonanSymbolsOverFir
import org.jetbrains.kotlin.backend.konan.serialization.KonanIdSignaturer
import org.jetbrains.kotlin.backend.konan.serialization.KonanManglerDesc
import org.jetbrains.kotlin.backend.konan.serialization.KonanManglerIr
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.fir.FirDiagnosticsCompilerResultsReporter
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.deserialization.PlatformDependentTypeTransformer
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.descriptors.konan.isNativeStdlib
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.fir.backend.Fir2IrExtensions
import org.jetbrains.kotlin.fir.backend.Fir2IrPluginContext
import org.jetbrains.kotlin.fir.backend.Fir2IrVisibilityConverter
import org.jetbrains.kotlin.fir.descriptors.FirModuleDescriptor
import org.jetbrains.kotlin.fir.pipeline.convertToIrAndActualize
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.library.metadata.KlibMetadataFactories
import org.jetbrains.kotlin.storage.LockBasedStorageManager

internal val KlibFactories = KlibMetadataFactories(::KonanBuiltIns, DynamicTypeDeserializer, PlatformDependentTypeTransformer.None)

internal fun PhaseContext.fir2Ir(
        input: FirOutput.Full,
): Fir2IrOutput {
    val fir2IrExtensions = Fir2IrExtensions.Default

    var builtInsModule: KotlinBuiltIns? = null
    val dependencies = mutableListOf<ModuleDescriptorImpl>()

    val resolvedLibraries = config.resolvedLibraries.getFullResolvedList()
    val configuration = config.configuration
    val librariesDescriptors = resolvedLibraries.map { resolvedLibrary ->
        val storageManager = LockBasedStorageManager("ModulesStructure")

        val moduleDescriptor = KlibFactories.DefaultDeserializedDescriptorFactory.createDescriptorOptionalBuiltIns(
                resolvedLibrary.library,
                configuration.languageVersionSettings,
                storageManager,
                builtInsModule,
                packageAccessHandler = null,
                lookupTracker = LookupTracker.DO_NOTHING
        )
        dependencies += moduleDescriptor

        val isBuiltIns = moduleDescriptor.isNativeStdlib()
        if (isBuiltIns) builtInsModule = moduleDescriptor.builtIns

        moduleDescriptor
    }

    librariesDescriptors.forEach { moduleDescriptor ->
        // Yes, just to all of them.
        moduleDescriptor.setDependencies(ArrayList(dependencies))
    }
    val diagnosticsReporter = DiagnosticReporterFactory.createPendingReporter()

    val (irModuleFragment, components, pluginContext, irActualizationResult) = input.firResult.convertToIrAndActualize(
            fir2IrExtensions,
            IrGenerationExtension.getInstances(config.project),
            linkViaSignatures = false,
            signatureComposerCreator = null,
            irMangler = KonanManglerIr,
            visibilityConverter = Fir2IrVisibilityConverter.Default,
            diagnosticReporter = diagnosticsReporter,
            languageVersionSettings = configuration.languageVersionSettings,
            kotlinBuiltIns = builtInsModule ?: DefaultBuiltIns.Instance,
    ).also {
        (it.irModuleFragment.descriptor as? FirModuleDescriptor)?.let { it.allDependencyModules = librariesDescriptors }
    }
    assert(irModuleFragment.name.isSpecial) {
        "`${irModuleFragment.name}` must be Name.special, since it's required by KlibMetadataModuleDescriptorFactoryImpl.createDescriptorOptionalBuiltIns()"
    }

    val symbols = createKonanSymbols(irModuleFragment, components, pluginContext)
    // TODO KT-55580 Invoke CopyDefaultValuesToActualPhase, same as PsiToir phase does.

    val renderDiagnosticNames = configuration.getBoolean(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME)
    FirDiagnosticsCompilerResultsReporter.reportToMessageCollector(diagnosticsReporter, messageCollector, renderDiagnosticNames)

    if (diagnosticsReporter.hasErrors) {
        throw KonanCompilationException("Compilation failed: there were some diagnostics during fir2ir")
    }

    return Fir2IrOutput(input.firResult, symbols, irModuleFragment, components, pluginContext, irActualizationResult)
}

private fun PhaseContext.createKonanSymbols(
        irModuleFragment: IrModuleFragment,
        components: Fir2IrComponents,
        pluginContext: Fir2IrPluginContext,
): KonanSymbolsOverFir {
    val moduleDescriptor = irModuleFragment.descriptor
    val symbolTable = SymbolTable(KonanIdSignaturer(KonanManglerDesc), pluginContext.irFactory)
    val descriptorsLookup = DescriptorsLookup(moduleDescriptor.builtIns as KonanBuiltIns)

    return KonanSymbolsOverFir(this, descriptorsLookup, components.irBuiltIns, symbolTable, symbolTable.lazyWrapper)
}
