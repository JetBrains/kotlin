package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.serialization.mangle.ManglerChecker
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
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.constant.EvaluatedConstTracker
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
import org.jetbrains.kotlin.fir.signaturer.Ir2FirManglerAdapter
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrExternalPackageFragment
import org.jetbrains.kotlin.ir.declarations.IrMemberWithContainerSource
import org.jetbrains.kotlin.ir.objcinterop.IrObjCOverridabilityCondition
import org.jetbrains.kotlin.ir.types.IrTypeSystemContextImpl
import org.jetbrains.kotlin.ir.util.DelicateSymbolTableApi
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.library.isNativeStdlib
import org.jetbrains.kotlin.library.metadata.KlibMetadataFactories
import org.jetbrains.kotlin.name.NativeForwardDeclarationKind
import org.jetbrains.kotlin.storage.LockBasedStorageManager

internal val KlibFactories = KlibMetadataFactories(::KonanBuiltIns, DynamicTypeDeserializer)

internal object NativeFir2IrExtensions : Fir2IrExtensions {
    override val irNeedsDeserialization = false
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

    val fir2IrConfiguration = Fir2IrConfiguration(
            languageVersionSettings = configuration.languageVersionSettings,
            diagnosticReporter = diagnosticsReporter,
            linkViaSignatures = true,
            evaluatedConstTracker = configuration
                    .putIfAbsent(CommonConfigurationKeys.EVALUATED_CONST_TRACKER, EvaluatedConstTracker.create()),
            inlineConstTracker = null,
            expectActualTracker = configuration[CommonConfigurationKeys.EXPECT_ACTUAL_TRACKER],
            allowNonCachedDeclarations = false,
            useIrFakeOverrideBuilder = configuration.getBoolean(CommonConfigurationKeys.USE_IR_FAKE_OVERRIDE_BUILDER),
    )
    val (irModuleFragment, components, pluginContext, irActualizedResult) = input.firResult.convertToIrAndActualize(
            NativeFir2IrExtensions,
            fir2IrConfiguration,
            IrGenerationExtension.getInstances(config.project),
            signatureComposer = DescriptorSignatureComposerStub(KonanManglerDesc),
            irMangler = KonanManglerIr,
            firMangler = FirNativeKotlinMangler,
            visibilityConverter = Fir2IrVisibilityConverter.Default,
            kotlinBuiltIns = builtInsModule ?: DefaultBuiltIns.Instance,
            actualizerTypeContextProvider = ::IrTypeSystemContextImpl,
            fir2IrResultPostCompute = { _, irOutput ->
                // it's important to compare manglers before actualization, since IR will be actualized, while FIR won't
                irOutput.irModuleFragment.acceptVoid(
                        ManglerChecker(
                                KonanManglerIr,
                                Ir2FirManglerAdapter(FirNativeKotlinMangler),
                                needsChecking = { false }, // FIXME(KT-60648): Re-enable
                        )
                )
            }
    ).also {
        (it.irModuleFragment.descriptor as? FirModuleDescriptor)?.let { it.allDependencyModules = librariesDescriptors }
    }
    assert(irModuleFragment.name.isSpecial) {
        "`${irModuleFragment.name}` must be Name.special, since it's required by KlibMetadataModuleDescriptorFactoryImpl.createDescriptorOptionalBuiltIns()"
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class, DelicateSymbolTableApi::class)
    val usedPackages = buildSet {
        components.symbolTable.descriptorExtension.forEachDeclarationSymbol {
            val p = it.owner as? IrDeclaration ?: return@forEachDeclarationSymbol
            val fragment = (p.getPackageFragment() as? IrExternalPackageFragment) ?: return@forEachDeclarationSymbol
            add(fragment.packageFqName)
        }
        // This packages exists in all platform libraries, but can contain only synthetic declarations.
        // These declarations are not really located in klib, so we don't need to depend on klib to use them.
        removeAll(NativeForwardDeclarationKind.entries.map { it.packageFqName })
    }.toList()


    val usedLibraries = librariesDescriptors.zip(resolvedLibraries).filter { (module, _) ->
        usedPackages.any { !module.packageFragmentProviderForModuleContentWithoutDependencies.isEmpty(it) }
    }.map { it.second }.toSet()

    resolvedLibraries.find { it.library.isNativeStdlib }?.let {
        require(usedLibraries.contains(it)) {
            "Internal error: stdlib must be in usedLibraries, if it's in resolvedLibraries"
        }
    }

    val symbols = createKonanSymbols(components, pluginContext)

    val renderDiagnosticNames = configuration.getBoolean(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME)
    FirDiagnosticsCompilerResultsReporter.reportToMessageCollector(diagnosticsReporter, messageCollector, renderDiagnosticNames)

    if (diagnosticsReporter.hasErrors) {
        throw KonanCompilationException("Compilation failed: there were some diagnostics during fir2ir")
    }

    return Fir2IrOutput(input.firResult, symbols, irModuleFragment, components, pluginContext, irActualizedResult, usedLibraries)
}

private fun PhaseContext.createKonanSymbols(
        components: Fir2IrComponents,
        pluginContext: Fir2IrPluginContext,
): KonanSymbols {
    val symbolTable = SymbolTable(KonanIdSignaturer(KonanManglerDesc), pluginContext.irFactory)

    return KonanSymbols(this, SymbolOverIrLookupUtils(), components.irBuiltIns, symbolTable.lazyWrapper)
}
