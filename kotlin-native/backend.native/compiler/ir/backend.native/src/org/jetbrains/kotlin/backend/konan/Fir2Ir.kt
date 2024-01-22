package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.serialization.metadata.DynamicTypeDeserializer
import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.backend.konan.driver.phases.Fir2IrOutput
import org.jetbrains.kotlin.backend.konan.driver.phases.FirOutput
import org.jetbrains.kotlin.backend.konan.ir.KonanSymbols
import org.jetbrains.kotlin.backend.konan.ir.SymbolOverIrLookupUtils
import org.jetbrains.kotlin.backend.konan.serialization.KonanIdSignaturer
import org.jetbrains.kotlin.backend.konan.serialization.KonanManglerDesc
import org.jetbrains.kotlin.backend.konan.serialization.KonanManglerIr
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.fir.FirDiagnosticsCompilerResultsReporter
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.isEmpty
import org.jetbrains.kotlin.descriptors.konan.isNativeStdlib
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.*
import org.jetbrains.kotlin.fir.backend.native.FirNativeKotlinMangler
import org.jetbrains.kotlin.fir.backend.native.interop.isExternalObjCClassProperty
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.utils.isLateInit
import org.jetbrains.kotlin.fir.descriptors.FirModuleDescriptor
import org.jetbrains.kotlin.fir.pipeline.convertToIrAndActualize
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrExternalPackageFragment
import org.jetbrains.kotlin.ir.declarations.IrMemberWithContainerSource
import org.jetbrains.kotlin.ir.objcinterop.IrObjCOverridabilityCondition
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrTypeSystemContextImpl
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.library.isNativeStdlib
import org.jetbrains.kotlin.library.metadata.KlibMetadataFactories
import org.jetbrains.kotlin.name.NativeForwardDeclarationKind
import org.jetbrains.kotlin.storage.LockBasedStorageManager

internal val KlibFactories = KlibMetadataFactories(::KonanBuiltIns, DynamicTypeDeserializer)

internal object NativeFir2IrExtensions : Fir2IrExtensions {
    override val irNeedsDeserialization = false
    override val parametersAreAssignable: Boolean get() = false
    override val externalOverridabilityConditions = listOf(IrObjCOverridabilityCondition)
    override fun generateOrGetFacadeClass(declaration: IrMemberWithContainerSource, components: Fir2IrComponents) = null
    override fun deserializeToplevelClass(irClass: IrClass, components: Fir2IrComponents) = false
    override fun registerDeclarations(symbolTable: SymbolTable) {}
    override fun findInjectedValue(calleeReference: FirReference, conversionScope: Fir2IrConversionScope) = null
    override fun hasBackingField(property: FirProperty, session: FirSession): Boolean =
            if (property.isExternalObjCClassProperty(session))
                property.isLateInit
            else
                Fir2IrExtensions.Default.hasBackingField(property, session)
}

internal fun PhaseContext.fir2Ir(
        input: FirOutput.Full,
): Fir2IrOutput {
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

    val fir2IrConfiguration = Fir2IrConfiguration.forKlibCompilation(configuration, diagnosticsReporter)
    val actualizedResult = input.firResult.convertToIrAndActualize(
            NativeFir2IrExtensions,
            fir2IrConfiguration,
            IrGenerationExtension.getInstances(config.project),
            irMangler = KonanManglerIr,
            firMangler = FirNativeKotlinMangler,
            visibilityConverter = Fir2IrVisibilityConverter.Default,
            kotlinBuiltIns = builtInsModule ?: DefaultBuiltIns.Instance,
            actualizerTypeContextProvider = ::IrTypeSystemContextImpl,
    ).also {
        (it.irModuleFragment.descriptor as? FirModuleDescriptor)?.let { it.allDependencyModules = librariesDescriptors }
    }
    assert(actualizedResult.irModuleFragment.name.isSpecial) {
        "`${actualizedResult.irModuleFragment.name}` must be Name.special, since it's required by KlibMetadataModuleDescriptorFactoryImpl.createDescriptorOptionalBuiltIns()"
    }

    @OptIn(DelicateDeclarationStorageApi::class)
    val usedPackages = buildSet {
        fun addExternalPackage(it: IrSymbol) {
            // FIXME(KT-64742): Fir2IrDeclarationStorage caches may contain unbound IR symbols, so we filter them out.
            val p = it.takeIf { it.isBound }?.owner as? IrDeclaration ?: return
            val fragment = (p.getPackageFragment() as? IrExternalPackageFragment) ?: return
            add(fragment.packageFqName)
        }
        actualizedResult.components.declarationStorage.forEachCachedDeclarationSymbol(::addExternalPackage)
        actualizedResult.components.classifierStorage.forEachCachedDeclarationSymbol(::addExternalPackage)

        // These packages exist in all platform libraries, but can contain only synthetic declarations.
        // These declarations are not really located in klib, so we don't need to depend on klib to use them.
        removeAll(NativeForwardDeclarationKind.entries.map { it.packageFqName }.toSet())
    }.toList()


    val usedLibraries = librariesDescriptors.zip(resolvedLibraries).filter { (module, _) ->
        usedPackages.any { !module.packageFragmentProviderForModuleContentWithoutDependencies.isEmpty(it) }
    }.map { it.second }.toSet()

    resolvedLibraries.find { it.library.isNativeStdlib }?.let {
        require(usedLibraries.contains(it)) {
            "Internal error: stdlib must be in usedLibraries, if it's in resolvedLibraries"
        }
    }

    val symbols = createKonanSymbols(actualizedResult.components, actualizedResult.pluginContext)

    val renderDiagnosticNames = configuration.getBoolean(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME)
    FirDiagnosticsCompilerResultsReporter.reportToMessageCollector(diagnosticsReporter, messageCollector, renderDiagnosticNames)

    if (diagnosticsReporter.hasErrors) {
        throw KonanCompilationException("Compilation failed: there were some diagnostics during fir2ir")
    }

    return Fir2IrOutput(input.firResult, symbols, actualizedResult, usedLibraries)
}

private fun PhaseContext.createKonanSymbols(
        components: Fir2IrComponents,
        pluginContext: Fir2IrPluginContext,
): KonanSymbols {
    val symbolTable = SymbolTable(KonanIdSignaturer(KonanManglerDesc), pluginContext.irFactory)

    return KonanSymbols(this, SymbolOverIrLookupUtils(), components.irBuiltIns, symbolTable.lazyWrapper)
}
