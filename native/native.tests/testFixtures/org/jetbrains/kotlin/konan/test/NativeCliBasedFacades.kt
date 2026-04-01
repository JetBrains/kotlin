/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.cli.common.diagnosticsCollector
import org.jetbrains.kotlin.cli.pipeline.withNewDiagnosticCollector
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.diagnostics.impl.DiagnosticsCollectorImpl
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.isNativeStdlib
import org.jetbrains.kotlin.library.loader.KlibLoader
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
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.*
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
    override val additionalServices: List<ServiceRegistrationData>
        get() = super.additionalServices + listOf(service(::ModuleDescriptorProvider))

    override fun transform(
        module: TestModule,
        inputArtifact: IrBackendInput,
    ): BinaryArtifacts.KLib? {
        val cliArtifact = inputArtifact.extractCliBasedArtifact()
        val diagnosticsCollector = DiagnosticsCollectorImpl()
        val input = cliArtifact.withNewDiagnosticCollector(diagnosticsCollector)
        val serializedOutput = NativeIrSerializationPipelinePhase.executePhase(input) ?: return null
        val output = NativeKlibWritingPipelinePhase.executePhase(serializedOutput)

        updateTestConfiguration(
            module = module,
            outputKlibPath = output.outputKlibPath,
            dependencyLibraries = input.phaseContext.config.loadedKlibs.all,
            languageVersionSettings = input.configuration.languageVersionSettings,
        )

        return BinaryArtifacts.KLib(File(output.outputKlibPath), input.configuration.diagnosticsCollector)
    }

    private fun updateTestConfiguration(
        module: TestModule,
        outputKlibPath: String,
        dependencyLibraries: Collection<KotlinLibrary>,
        languageVersionSettings: LanguageVersionSettings,
    ) {
        val (builtIns, dependencyModuleDescriptors) = loadDependencies(dependencyLibraries, languageVersionSettings)

        val libraryLoadingResult = KlibLoader { libraryPaths(outputKlibPath) }.load()
        testServices.assertions.assertTrue(!libraryLoadingResult.hasProblems && libraryLoadingResult.librariesStdlibFirst.size == 1) {
            "Failed to load just compiled library: $outputKlibPath"
        }

        val library = libraryLoadingResult.librariesStdlibFirst.single()

        val moduleDescriptor = klibFactories.DefaultDeserializedDescriptorFactory.createDescriptorOptionalBuiltIns(
            library,
            languageVersionSettings,
            // TODO: check safety of the approach of creating a separate storage manager per library
            LockBasedStorageManager("ModulesStructure"),
            builtIns,
            lookupTracker = LookupTracker.DO_NOTHING
        )
        moduleDescriptor.setDependencies(dependencyModuleDescriptors + moduleDescriptor)

        testServices.libraryProvider.setDescriptorAndLibraryByName(outputKlibPath, moduleDescriptor, library)
        testServices.moduleDescriptorProvider.replaceModuleDescriptorForModule(module, moduleDescriptor)
    }

    private fun loadDependencies(
        dependencyLibraries: Collection<KotlinLibrary>,
        languageVersionSettings: LanguageVersionSettings,
    ): Pair<KotlinBuiltIns?, List<ModuleDescriptorImpl>> {
        val allModuleDescriptors = ArrayList<ModuleDescriptorImpl>()
        val createdModuleDescriptors = ArrayList<ModuleDescriptorImpl>()

        val stdlib: KotlinLibrary? = dependencyLibraries.firstOrNull { it.isNativeStdlib }
        var builtIns: KotlinBuiltIns? = null

        fun loadOrCreateModuleDescriptor(library: KotlinLibrary): ModuleDescriptorImpl {
            val moduleDescriptor = testServices.libraryProvider.getOrCreateStdlibByPath(library.libraryFile.absolutePath) {
                val moduleDescriptor = klibFactories.DefaultDeserializedDescriptorFactory.createDescriptorOptionalBuiltIns(
                    library,
                    languageVersionSettings,
                    // TODO: check safety of the approach of creating a separate storage manager per library
                    LockBasedStorageManager("ModulesStructure"),
                    builtIns,
                    lookupTracker = LookupTracker.DO_NOTHING
                )

                createdModuleDescriptors += moduleDescriptor

                Pair(moduleDescriptor, library)
            } as ModuleDescriptorImpl

            allModuleDescriptors += moduleDescriptor

            return moduleDescriptor
        }

        // first, create or load stdlib
        builtIns = stdlib?.let { loadOrCreateModuleDescriptor(it) }?.builtIns

        // then, other dependencies
        for (library in dependencyLibraries) {
            if (library == stdlib) continue
            loadOrCreateModuleDescriptor(library)
        }

        // for all newly created modules, set dependencies
        for (moduleDescriptor in createdModuleDescriptors) {
            moduleDescriptor.setDependencies(allModuleDescriptors.toList())
        }

        return builtIns to allModuleDescriptors
    }

    override fun shouldTransform(module: TestModule): Boolean {
        return testServices.defaultsProvider.backendKind == inputKind && SKIP_GENERATING_KLIB !in module.directives
    }

    companion object {
        private val klibFactories = KlibMetadataFactories(::KonanBuiltIns, NullFlexibleTypeDeserializer)
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
