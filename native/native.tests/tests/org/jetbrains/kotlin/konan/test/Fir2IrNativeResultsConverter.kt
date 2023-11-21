/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test

import org.jetbrains.kotlin.backend.common.serialization.metadata.DynamicTypeDeserializer
import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureDescriptor
import org.jetbrains.kotlin.backend.konan.serialization.KonanManglerDesc
import org.jetbrains.kotlin.backend.konan.serialization.KonanManglerIr
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.constant.EvaluatedConstTracker
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.fir.backend.Fir2IrConfiguration
import org.jetbrains.kotlin.fir.backend.Fir2IrExtensions
import org.jetbrains.kotlin.fir.backend.Fir2IrVisibilityConverter
import org.jetbrains.kotlin.fir.backend.native.FirNativeKotlinMangler
import org.jetbrains.kotlin.fir.descriptors.FirModuleDescriptor
import org.jetbrains.kotlin.fir.pipeline.convertToIrAndActualize
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.ir.types.IrTypeSystemContextImpl
import org.jetbrains.kotlin.library.metadata.KlibMetadataFactories
import org.jetbrains.kotlin.library.metadata.resolver.KotlinResolvedLibrary
import org.jetbrains.kotlin.library.unresolvedDependencies
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.frontend.fir.*
import org.jetbrains.kotlin.test.model.BackendKinds
import org.jetbrains.kotlin.test.model.Frontend2BackendConverter
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import org.jetbrains.kotlin.test.services.libraryProvider

class Fir2IrNativeResultsConverter(
    testServices: TestServices
) : Frontend2BackendConverter<FirOutputArtifact, IrBackendInput>(
    testServices,
    FrontendKinds.FIR,
    BackendKinds.IrBackend
) {

    override fun transform(module: TestModule, inputArtifact: FirOutputArtifact): IrBackendInput? =
        try {
            transformInternal(module, inputArtifact)
        } catch (e: Throwable) {
            if (CodegenTestDirectives.IGNORE_FIR2IR_EXCEPTIONS_IF_FIR_CONTAINS_ERRORS in module.directives && inputArtifact.hasErrors) {
                null
            } else {
                throw e
            }
        }

    private fun transformInternal(
        module: TestModule,
        inputArtifact: FirOutputArtifact
    ): IrBackendInput {
        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)

        val libraries = resolveLibraries(configuration, getAllNativeDependenciesPaths(module, testServices))
        val (dependencies, builtIns) = loadResolvedLibraries(libraries, configuration.languageVersionSettings, testServices)

        val diagnosticReporter = DiagnosticReporterFactory.createReporter()
        val fir2IrConfiguration = Fir2IrConfiguration(
            languageVersionSettings = configuration.languageVersionSettings,
            diagnosticReporter = diagnosticReporter,
            linkViaSignatures = true,
            evaluatedConstTracker = configuration
                .putIfAbsent(CommonConfigurationKeys.EVALUATED_CONST_TRACKER, EvaluatedConstTracker.create()),
            inlineConstTracker = null,
            allowNonCachedDeclarations = false,
            expectActualTracker = configuration[CommonConfigurationKeys.EXPECT_ACTUAL_TRACKER],
            useIrFakeOverrideBuilder = configuration.getBoolean(CommonConfigurationKeys.USE_IR_FAKE_OVERRIDE_BUILDER),
        )
        val fir2irResult = inputArtifact.toFirResult().convertToIrAndActualize(
            Fir2IrExtensions.Default,
            fir2IrConfiguration,
            module.irGenerationExtensions(testServices),
            IdSignatureDescriptor(KonanManglerDesc),
            KonanManglerIr,
            FirNativeKotlinMangler,
            Fir2IrVisibilityConverter.Default,
            builtIns ?: DefaultBuiltIns.Instance, // TODO: consider passing externally,
            ::IrTypeSystemContextImpl
        ).also {
            (it.irModuleFragment.descriptor as? FirModuleDescriptor)?.let { it.allDependencyModules = dependencies }
        }

        return IrBackendInput.NativeBackendInput(
            fir2irResult.irModuleFragment,
            fir2irResult.pluginContext,
            diagnosticReporter = diagnosticReporter,
            descriptorMangler = fir2irResult.components.symbolTable.signaturer.mangler,
            irMangler = KonanManglerIr,
            firMangler = fir2irResult.components.signatureComposer.mangler,
        )
    }
}

internal val KlibFactories = KlibMetadataFactories(::KonanBuiltIns, DynamicTypeDeserializer)

private fun loadResolvedLibraries(
    resolvedLibraries: List<KotlinResolvedLibrary>,
    languageVersionSettings: LanguageVersionSettings,
    testServices: TestServices
): Pair<List<ModuleDescriptor>, KotlinBuiltIns?> {
    var builtInsModule: KotlinBuiltIns? = null
    val dependencies = mutableListOf<ModuleDescriptorImpl>()

    return resolvedLibraries.map { resolvedLibrary ->
        testServices.libraryProvider.getOrCreateStdlibByPath(resolvedLibrary.library.libraryFile.absolutePath) {
            // TODO: check safety of the approach of creating a separate storage manager per library
            val storageManager = LockBasedStorageManager("ModulesStructure")

            val moduleDescriptor = KlibFactories.DefaultDeserializedDescriptorFactory.createDescriptorOptionalBuiltIns(
                resolvedLibrary.library,
                languageVersionSettings,
                storageManager,
                builtInsModule,
                packageAccessHandler = null,
                lookupTracker = LookupTracker.DO_NOTHING
            )
            dependencies += moduleDescriptor
            moduleDescriptor.setDependencies(ArrayList(dependencies))

            Pair(moduleDescriptor, resolvedLibrary.library)
        }.also {
            val isBuiltIns = resolvedLibrary.library.unresolvedDependencies.isEmpty()
            if (isBuiltIns) builtInsModule = it.builtIns
        }
    } to builtInsModule
}
