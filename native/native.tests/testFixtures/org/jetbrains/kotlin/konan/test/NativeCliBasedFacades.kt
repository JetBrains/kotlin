/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test

import org.jetbrains.kotlin.backend.common.serialization.metadata.DynamicTypeDeserializer
import org.jetbrains.kotlin.backend.konan.serialization.loadNativeKlibsInTestPipeline
import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.cli.pipeline.withNewDiagnosticCollector
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.diagnostics.impl.DiagnosticsCollectorImpl
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.library.metadata.KlibMetadataFactories
import org.jetbrains.kotlin.library.metadata.NullFlexibleTypeDeserializer
import org.jetbrains.kotlin.native.pipeline.*
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.test.backend.ir.IrBackendFacade
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.directives.KlibBasedCompilerTestDirectives.SKIP_GENERATING_KLIB
import org.jetbrains.kotlin.test.frontend.classic.moduleDescriptorProvider
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrCliBasedOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrCliFacade
import org.jetbrains.kotlin.test.frontend.fir.FirCliFacade
import org.jetbrains.kotlin.test.frontend.fir.getAllNativeDependenciesPaths
import org.jetbrains.kotlin.test.frontend.fir.getTransitivesAndFriendsPaths
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.NativeEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.nativeEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.defaultsProvider
import org.jetbrains.kotlin.test.services.libraryProvider
import java.io.File

// NativeCliBasedFacades

class FirCliNativeFacade(
    testServices: TestServices
) : FirCliFacade<NativeFrontendPipelinePhase, NativeFrontendArtifact>(testServices, NativeFrontendPipelinePhase)

class Fir2IrCliNativeFacade(
    testServices: TestServices
) : Fir2IrCliFacade<NativeFir2IrPipelinePhase, NativeFrontendArtifact, NativeFir2IrArtifact>(testServices, NativeFir2IrPipelinePhase)

class NativePreSerializationLoweringCliFacade(
    testServices: TestServices
) : IrPreSerializationLoweringFacade<IrBackendInput>(testServices, BackendKinds.IrBackend, BackendKinds.IrBackend) {
    override fun transform(
        module: TestModule,
        inputArtifact: IrBackendInput,
    ): IrBackendInput {
        val cliArtifact = inputArtifact.extractCliBasedArtifact()
        val input = cliArtifact.withNewDiagnosticCollector(DiagnosticsCollectorImpl())
        val output = NativePreSerializationPipelinePhase.executePhase(input)
        return Fir2IrCliBasedOutputArtifact(output)
    }

    override fun shouldTransform(module: TestModule): Boolean {
        return true
    }
}

class KlibSerializerNativeCliFacade(
    testServices: TestServices
) : IrBackendFacade<BinaryArtifacts.KLib>(testServices, ArtifactKinds.KLib) {
    override fun transform(
        module: TestModule,
        inputArtifact: IrBackendInput,
    ): BinaryArtifacts.KLib? {
        val cliArtifact = inputArtifact.extractCliBasedArtifact()
        val diagnosticsCollector = DiagnosticsCollectorImpl()
        val input = cliArtifact.withNewDiagnosticCollector(diagnosticsCollector)
        val serializedOutput = NativeIrSerializationPipelinePhase.executePhase(input) ?: return null
        val output = NativeKlibWritingPipelinePhase.executePhase(serializedOutput)
        val outputFile = File(output.outputKlibPath)
        updateTestConfiguration(input.configuration, module, input.fir2IrOutput.fir2irActualizedResult.irModuleFragment, outputFile)
        return BinaryArtifacts.KLib(outputFile, diagnosticsCollector)
    }

    private fun updateTestConfiguration(
        configuration: CompilerConfiguration,
        module: TestModule,
        irModuleFragment: IrModuleFragment,
        outputFile: File
    ) {
        val nativeFactories = KlibMetadataFactories(::KonanBuiltIns, NullFlexibleTypeDeserializer)

        val dependencyPaths = getAllNativeDependenciesPaths(module, testServices)

        val library = loadNativeKlibsInTestPipeline(
            configuration = configuration,
            libraryPaths = listOf(outputFile.path),
            nativeTarget = testServices.nativeEnvironmentConfigurator.getNativeTarget(module),
        ).all.single()

        val moduleDescriptor = nativeFactories.DefaultDeserializedDescriptorFactory.createDescriptorOptionalBuiltIns(
            library,
            configuration.languageVersionSettings,
            LockBasedStorageManager("ModulesStructure"),
            irModuleFragment.descriptor.builtIns,
            packageAccessHandler = null,
            lookupTracker = LookupTracker.DO_NOTHING
        )

        val descriptorDependencies = buildList {
            val klibFactories = KlibMetadataFactories(::KonanBuiltIns, DynamicTypeDeserializer)
            NativeEnvironmentConfigurator.getRuntimePathsForModule(module, testServices).mapTo(this) {
                testServices.libraryProvider.getOrCreateStdlibByPath(it) {
                    // TODO: check safety of the approach of creating a separate storage manager per library
                    val storageManager = LockBasedStorageManager("ModulesStructure")

                    val moduleDescriptor = klibFactories.DefaultDeserializedDescriptorFactory.createDescriptorOptionalBuiltIns(
                        library,
                        configuration.languageVersionSettings,
                        storageManager,
                        irModuleFragment.descriptor.builtIns,
                        packageAccessHandler = null,
                        lookupTracker = LookupTracker.DO_NOTHING
                    )

                    moduleDescriptor to library
                } as ModuleDescriptorImpl
            }
            getTransitivesAndFriendsPaths(module, testServices).mapTo(this) { testServices.libraryProvider.getDescriptorByPath(it) as ModuleDescriptorImpl }
            add(moduleDescriptor)
        }
        moduleDescriptor.setDependencies(descriptorDependencies)

        testServices.moduleDescriptorProvider.replaceModuleDescriptorForModule(module, moduleDescriptor)
        testServices.libraryProvider.setDescriptorAndLibraryByName(outputFile.path, moduleDescriptor, library)
    }

    override fun shouldTransform(module: TestModule): Boolean {
        return testServices.defaultsProvider.backendKind == inputKind && SKIP_GENERATING_KLIB !in module.directives
    }
}

private fun IrBackendInput.extractCliBasedArtifact(): NativeFir2IrArtifact {
    require(this is Fir2IrCliBasedOutputArtifact<*>) {
        "NativePreSerializationLoweringCliFacade expects Fir2IrCliBasedOutputArtifact as input, got ${this::class.simpleName}"
    }
    val cliArtifact = this.cliArtifact
    require(cliArtifact is NativeFir2IrArtifact) {
        "FirKlibSerializerCliWebFacade expects NativeFir2IrArtifact as input"
    }
    return cliArtifact
}
