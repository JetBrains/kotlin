/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend

import com.intellij.openapi.project.Project
import org.jebrains.kotlin.backend.native.PhaseContext
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.serialization.metadata.DynamicTypeDeserializer
import org.jetbrains.kotlin.backend.konan.KonanCompilationException
import org.jetbrains.kotlin.backend.konan.ir.KonanSymbols
import org.jetbrains.kotlin.backend.konan.serialization.KonanManglerIr
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.fir.FirDiagnosticsCompilerResultsReporter
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.isEmpty
import org.jetbrains.kotlin.descriptors.konan.isNativeStdlib
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.fir.backend.DelicateDeclarationStorageApi
import org.jetbrains.kotlin.fir.backend.Fir2IrConfiguration
import org.jetbrains.kotlin.fir.backend.Fir2IrVisibilityConverter
import org.jetbrains.kotlin.fir.descriptors.FirModuleDescriptor
import org.jetbrains.kotlin.fir.pipeline.Fir2IrActualizedResult
import org.jetbrains.kotlin.fir.pipeline.FirResult
import org.jetbrains.kotlin.fir.pipeline.convertToIrAndActualize
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrExternalPackageFragment
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrTypeSystemContextImpl
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.library.isNativeStdlib
import org.jetbrains.kotlin.library.metadata.KlibMetadataFactories
import org.jetbrains.kotlin.library.metadata.resolver.KotlinLibraryResolveResult
import org.jetbrains.kotlin.library.metadata.resolver.KotlinResolvedLibrary
import org.jetbrains.kotlin.name.NativeForwardDeclarationKind
import org.jetbrains.kotlin.storage.LockBasedStorageManager

internal val KlibFactories = KlibMetadataFactories(::KonanBuiltIns, DynamicTypeDeserializer)

data class Fir2IrOutput(
        val firResult: FirResult,
        val symbols: KonanSymbols,
        val fir2irActualizedResult: Fir2IrActualizedResult,
        val usedLibraries: Set<KotlinResolvedLibrary>
)

data class Fir2IrInput(
        val firOutput: FirOutput.Full,
        val project: Project,
        val resolvedLibraries: KotlinLibraryResolveResult,
)

internal fun PhaseContext.fir2Ir(
        input: Fir2IrInput,
): Fir2IrOutput {
    var builtInsModule: KotlinBuiltIns? = null

    val resolvedLibraries = input.resolvedLibraries.getFullResolvedList()
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
    val messageCollector = configuration.getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY)
    val diagnosticsReporter = DiagnosticReporterFactory.createPendingReporter(messageCollector)

    val fir2IrConfiguration = Fir2IrConfiguration.forKlibCompilation(configuration, diagnosticsReporter)
    val actualizedResult = input.firOutput.firResult.convertToIrAndActualize(
            NativeFir2IrExtensions,
            fir2IrConfiguration,
            IrGenerationExtension.getInstances(input.project),
            irMangler = KonanManglerIr,
            visibilityConverter = Fir2IrVisibilityConverter.Default,
            kotlinBuiltIns = builtInsModule ?: DefaultBuiltIns.Instance,
            specialAnnotationsProvider = null,
            extraActualDeclarationExtractorsInitializer = { emptyList() },
            typeSystemContextProvider = ::IrTypeSystemContextImpl,
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

    val symbols = createKonanSymbols(actualizedResult.irBuiltIns)

    val renderDiagnosticNames = configuration.getBoolean(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME)
    FirDiagnosticsCompilerResultsReporter.reportToMessageCollector(diagnosticsReporter, messageCollector, renderDiagnosticNames)

    if (diagnosticsReporter.hasErrors) {
        throw KonanCompilationException("Compilation failed: there were some diagnostics during fir2ir")
    }

    return Fir2IrOutput(input.firOutput.firResult, symbols, actualizedResult, usedLibraries)
}

private fun PhaseContext.createKonanSymbols(
        irBuiltIns: IrBuiltIns,
): KonanSymbols {
    return KonanSymbols(this, irBuiltIns, this.configuration)
}
