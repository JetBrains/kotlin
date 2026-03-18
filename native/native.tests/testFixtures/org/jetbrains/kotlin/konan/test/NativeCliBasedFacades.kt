/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test

import org.jetbrains.kotlin.backend.common.serialization.metadata.DynamicTypeDeserializer
import org.jetbrains.kotlin.backend.konan.serialization.loadNativeKlibsInTestPipeline
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.cli.common.diagnosticsCollector
import org.jetbrains.kotlin.cli.pipeline.withNewDiagnosticCollector
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.diagnostics.impl.DiagnosticsCollectorImpl
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.konan.library.isFromKotlinNativeDistribution
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.isNativeStdlib
import org.jetbrains.kotlin.library.metadata.KlibMetadataFactories
import org.jetbrains.kotlin.library.metadata.NullFlexibleTypeDeserializer
import org.jetbrains.kotlin.native.pipeline.*
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.test.backend.ir.IrBackendFacade
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.directives.KlibBasedCompilerTestDirectives.SKIP_GENERATING_KLIB
import org.jetbrains.kotlin.test.frontend.classic.ModuleDescriptorProvider
import org.jetbrains.kotlin.test.frontend.classic.moduleDescriptorProvider
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrCliBasedOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrCliFacade
import org.jetbrains.kotlin.test.frontend.fir.FirCliFacade
import org.jetbrains.kotlin.test.frontend.fir.getAllNativeDependenciesPaths
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.nativeEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.defaultsProvider
import org.jetbrains.kotlin.test.services.libraryProvider
import org.jetbrains.kotlin.test.services.service
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
        return BinaryArtifacts.KLib(outputFile, input.configuration.diagnosticsCollector)
    }

    private fun updateTestConfiguration(
        configuration: CompilerConfiguration,
        module: TestModule,
        irModuleFragment: IrModuleFragment,
        outputFile: File
    ) {
        val allLibraries = loadNativeKlibsInTestPipeline(
            configuration = configuration,
            runtimeLibraryProviders = testServices.nativeEnvironmentConfigurator.getRuntimeLibraryProviders(module),
            libraryPaths = listOf(outputFile.path),
            nativeTarget = testServices.nativeEnvironmentConfigurator.getNativeTarget(module),
        ).all
        val stdlibLibrary = allLibraries.single { it.isNativeStdlib }
        val moduleLibrary = allLibraries.single { !it.isFromKotlinNativeDistribution }

        fun createDescriptorOptionalBuiltIns(factories: KlibMetadataFactories, library: KotlinLibrary, builtIns: KotlinBuiltIns?): ModuleDescriptorImpl =
            factories.DefaultDeserializedDescriptorFactory.createDescriptorOptionalBuiltIns(
                library,
                configuration.languageVersionSettings,
                // TODO: check safety of the approach of creating a separate storage manager per library
                LockBasedStorageManager("ModulesStructure"),
                builtIns,

                lookupTracker = LookupTracker.DO_NOTHING
            )

        val stdlibModuleDescriptor = createDescriptorOptionalBuiltIns(
            KlibMetadataFactories(::KonanBuiltIns, DynamicTypeDeserializer),
            stdlibLibrary,
            builtIns = null, // create new KonanBuiltins for the first created ModuleDescriptorImpl
        ).also {
            it.setDependencies(listOf(it))
        }
        val moduleDescriptor = createDescriptorOptionalBuiltIns(
            KlibMetadataFactories(::KonanBuiltIns, NullFlexibleTypeDeserializer),
            moduleLibrary,
            stdlibModuleDescriptor.builtIns,
        )
        val descriptorDependencies = buildList {
            // TODO Now, the same stdlib module descriptor is used for each platformlib and transitive dependency as well,
            //      see identical values of map `testServices.libraryProvider.stdlibPathDoDescriptor` after calculation of `descriptorDependencies`.
            //      It's weird it works at all. Consider improving it please.
            getAllNativeDependenciesPaths(module, testServices).mapTo(this) {
                testServices.libraryProvider.getOrCreateStdlibByPath(it) {
                    stdlibModuleDescriptor to stdlibLibrary
                } as ModuleDescriptorImpl
            }
            add(moduleDescriptor)
        }
        moduleDescriptor.setDependencies(descriptorDependencies)

        testServices.register(service(::ModuleDescriptorProvider), skipAlreadyRegistered = true)
        testServices.moduleDescriptorProvider.replaceModuleDescriptorForModule(module, moduleDescriptor)
        testServices.libraryProvider.setDescriptorAndLibraryByName(outputFile.path, moduleDescriptor, moduleLibrary)
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
