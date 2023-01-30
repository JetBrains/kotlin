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
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.deserialization.PlatformDependentTypeTransformer
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.descriptors.konan.isNativeStdlib
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.*
import org.jetbrains.kotlin.fir.backend.jvm.Fir2IrJvmSpecialAnnotationSymbolProvider
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmKotlinMangler
import org.jetbrains.kotlin.fir.descriptors.FirModuleDescriptor
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirProviderImpl
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.library.metadata.KlibMetadataFactories
import org.jetbrains.kotlin.storage.LockBasedStorageManager

internal val KlibFactories = KlibMetadataFactories(::KonanBuiltIns, DynamicTypeDeserializer, PlatformDependentTypeTransformer.None)

internal fun PhaseContext.fir2Ir(
        input: FirOutput.Full,
): Fir2IrOutput {
    val fir2IrExtensions = Fir2IrExtensions.Default
    val commonFirFiles = input.session.moduleData.dependsOnDependencies
            .map { it.session }
            .filter { it.kind == FirSession.Kind.Source }
            .flatMap { (it.firProvider as FirProviderImpl).getAllFirFiles() }

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

    val commonMemberStorage = Fir2IrCommonMemberStorage(
        generateSignatures = false,
        signatureComposerCreator = null,
        manglerCreator = { FirJvmKotlinMangler() } // TODO: replace with potentially simpler JS version
    )

    val fir2irResult = Fir2IrConverter.createModuleFragmentWithSignaturesIfNeeded(
            input.session, input.scopeSession, input.firFiles + commonFirFiles,
            configuration.languageVersionSettings,
            fir2IrExtensions,
            KonanManglerIr, IrFactoryImpl,
            Fir2IrVisibilityConverter.Default,
            Fir2IrJvmSpecialAnnotationSymbolProvider(), // TODO: replace with appropriate (probably empty) implementation
            IrGenerationExtension.getInstances(config.project),
            generateSignatures = false,
            kotlinBuiltIns = builtInsModule ?: DefaultBuiltIns.Instance, // TODO: consider passing externally
            commonMemberStorage = commonMemberStorage,
            initializedIrBuiltIns = null
    ).also {
        (it.irModuleFragment.descriptor as? FirModuleDescriptor)?.let { it.allDependencyModules = librariesDescriptors }
    }

    val symbols = createKonanSymbols(fir2irResult)
    // TODO KT-55580 Invoke CopyDefaultValuesToActualPhase, same as PsiToir phase does.
    return Fir2IrOutput(input.session, input.scopeSession, input.firFiles, fir2irResult, symbols)
}

private fun PhaseContext.createKonanSymbols(
        fir2irResult: Fir2IrResult,
): KonanSymbolsOverFir {
    val moduleDescriptor = fir2irResult.irModuleFragment.descriptor
    val symbolTable = SymbolTable(KonanIdSignaturer(KonanManglerDesc), fir2irResult.pluginContext.irFactory)
    val descriptorsLookup = DescriptorsLookup(moduleDescriptor.builtIns as KonanBuiltIns)

    return KonanSymbolsOverFir(this, descriptorsLookup, fir2irResult.components.irBuiltIns, symbolTable, symbolTable.lazyWrapper)
}
