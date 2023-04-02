package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.serialization.mangle.ManglerChecker
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
import org.jetbrains.kotlin.descriptors.isEmpty
import org.jetbrains.kotlin.descriptors.konan.isNativeStdlib
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.fir.backend.*
import org.jetbrains.kotlin.fir.descriptors.FirModuleDescriptor
import org.jetbrains.kotlin.fir.pipeline.convertToIrAndActualize
import org.jetbrains.kotlin.fir.signaturer.Ir2FirManglerAdapter
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrExternalPackageFragment
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.library.metadata.KlibMetadataFactories
import org.jetbrains.kotlin.library.metadata.impl.ForwardDeclarationKind
import org.jetbrains.kotlin.storage.LockBasedStorageManager

internal val KlibFactories = KlibMetadataFactories(::KonanBuiltIns, DynamicTypeDeserializer, PlatformDependentTypeTransformer.None)

internal fun PhaseContext.fir2Ir(
        input: FirOutput.Full,
): Fir2IrOutput {
    val fir2IrExtensions = Fir2IrExtensions.Default

    var builtInsModule: KotlinBuiltIns? = null

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

        val isBuiltIns = moduleDescriptor.isNativeStdlib()
        if (isBuiltIns) builtInsModule = moduleDescriptor.builtIns

        moduleDescriptor
    }

    librariesDescriptors.forEach { moduleDescriptor ->
        // Yes, just to all of them.
        moduleDescriptor.setDependencies(ArrayList(librariesDescriptors))
    }
    val diagnosticsReporter = DiagnosticReporterFactory.createPendingReporter()

    val (irModuleFragment, components, pluginContext, irActualizationResult) = input.firResult.convertToIrAndActualize(
            fir2IrExtensions,
            IrGenerationExtension.getInstances(config.project),
            linkViaSignatures = false,
            signatureComposerCreator = null,
            irMangler = KonanManglerIr,
            firManglerCreator = { FirNativeKotlinMangler() },
            visibilityConverter = Fir2IrVisibilityConverter.Default,
            diagnosticReporter = diagnosticsReporter,
            languageVersionSettings = configuration.languageVersionSettings,
            kotlinBuiltIns = builtInsModule ?: DefaultBuiltIns.Instance,
            fir2IrResultPostCompute = {
                // it's important to compare manglers before actualization, since IR will be actualized, while FIR won't
                irModuleFragment.acceptVoid(
                        ManglerChecker(KonanManglerIr, Ir2FirManglerAdapter(FirNativeKotlinMangler()), needsChecking = ManglerChecker.hasMetadata)
                )
            }
    ).also {
        (it.irModuleFragment.descriptor as? FirModuleDescriptor)?.let { it.allDependencyModules = librariesDescriptors }
    }
    assert(irModuleFragment.name.isSpecial) {
        "`${irModuleFragment.name}` must be Name.special, since it's required by KlibMetadataModuleDescriptorFactoryImpl.createDescriptorOptionalBuiltIns()"
    }

    val usedPackages = buildSet {
        components.symbolTable.forEachDeclarationSymbol {
            val p = it.owner as? IrDeclaration ?: return@forEachDeclarationSymbol
            val fragment = (p.getPackageFragment() as? IrExternalPackageFragment) ?: return@forEachDeclarationSymbol
            add(fragment.fqName)
        }
        // This packages exists in all platform libraries, but can contain only synthetic declarations.
        // These declarations are not really located in klib, so we don't need to depend on klib to use them.
        removeAll(ForwardDeclarationKind.values().map { it.packageFqName })
    }.toList()


    val usedLibraries = librariesDescriptors.zip(resolvedLibraries).filter { (module, _) ->
        usedPackages.any { !module.packageFragmentProviderForModuleContentWithoutDependencies.isEmpty(it) }
    }.map { it.second }.toSet()


    val symbols = createKonanSymbols(irModuleFragment, components, pluginContext)

    val renderDiagnosticNames = configuration.getBoolean(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME)
    FirDiagnosticsCompilerResultsReporter.reportToMessageCollector(diagnosticsReporter, messageCollector, renderDiagnosticNames)

    if (diagnosticsReporter.hasErrors) {
        throw KonanCompilationException("Compilation failed: there were some diagnostics during fir2ir")
    }

    return Fir2IrOutput(input.firResult, symbols, irModuleFragment, components, pluginContext, irActualizationResult, usedLibraries)
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
