/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test

import org.jetbrains.kotlin.backend.common.klibAbiVersionForManifest
import org.jetbrains.kotlin.backend.common.serialization.IrSerializationSettings
import org.jetbrains.kotlin.backend.common.serialization.SerializerOutput
import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibMetadataMonolithicSerializer
import org.jetbrains.kotlin.backend.common.serialization.serializeModuleIntoKlib
import org.jetbrains.kotlin.backend.konan.serialization.KonanIrModuleSerializer
import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.ir.KtDiagnosticReporterWithImplicitIrBasedContext
import org.jetbrains.kotlin.konan.library.KLIB_INTEROP_IR_PROVIDER_IDENTIFIER
import org.jetbrains.kotlin.konan.library.impl.buildLibrary
import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.library.metadata.KlibMetadataFactories
import org.jetbrains.kotlin.library.metadata.NullFlexibleTypeDeserializer
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.test.backend.ir.IrBackendFacade
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.directives.KlibBasedCompilerTestDirectives.SKIP_GENERATING_KLIB
import org.jetbrains.kotlin.test.frontend.classic.ModuleDescriptorProvider
import org.jetbrains.kotlin.test.frontend.classic.moduleDescriptorProvider
import org.jetbrains.kotlin.test.frontend.fir.getAllNativeDependenciesPaths
import org.jetbrains.kotlin.test.frontend.fir.resolveLibraries
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.services.configuration.NativeEnvironmentConfigurator.Companion.getKlibArtifactFile
import org.jetbrains.kotlin.test.services.configuration.nativeEnvironmentConfigurator
import org.jetbrains.kotlin.util.klibMetadataVersionOrDefault
import java.io.File

abstract class AbstractNativeKlibSerializerFacade(
    testServices: TestServices
) : IrBackendFacade<BinaryArtifacts.KLib>(testServices, ArtifactKinds.KLib) {
    override val additionalServices: List<ServiceRegistrationData>
        get() = listOf(service(::ModuleDescriptorProvider))

    final override fun shouldTransform(module: TestModule): Boolean {
        return testServices.defaultsProvider.backendKind == inputKind && SKIP_GENERATING_KLIB !in module.directives
    }

    final override fun transform(module: TestModule, inputArtifact: IrBackendInput): BinaryArtifacts.KLib {
        require(inputArtifact is IrBackendInput.NativeAfterFrontendBackendInput) {
            "${this::class.java.simpleName} expects IrBackendInput.NativeAfterFrontendBackendInput as input"
        }

        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
        val diagnosticReporter = DiagnosticReporterFactory.createReporter(configuration.messageCollector)
        val outputFile = getKlibArtifactFile(testServices, module.name)

        serializeBare(module, inputArtifact, outputFile, configuration, diagnosticReporter)

        val outputArtifact = BinaryArtifacts.KLib(outputFile, diagnosticReporter)

        updateTestConfiguration(configuration, module, inputArtifact, outputArtifact)

        return outputArtifact
    }

    fun serializeBare(
        module: TestModule,
        inputArtifact: IrBackendInput,
        outputKlibArtifactFile: File,
        configuration: CompilerConfiguration,
        diagnosticReporter: BaseDiagnosticsCollector
    ) {
        require(inputArtifact is IrBackendInput.NativeAfterFrontendBackendInput) {
            "${this::class.java.simpleName} expects IrBackendInput.NativeAfterFrontendBackendInput as input"
        }

        val dependencies = inputArtifact.usedLibrariesForManifest
        val serializerOutput = serialize(configuration, dependencies, module, inputArtifact, diagnosticReporter)

        buildLibrary(
            natives = emptyList(),
            included = emptyList(),
            linkDependencies = serializerOutput.neededLibraries,
            serializerOutput.serializedMetadata ?: testServices.assertions.fail { "expected serialized metadata" },
            serializerOutput.serializedIr,
            versions = KotlinLibraryVersioning(
                compilerVersion = KotlinCompilerVersion.getVersion(),
                abiVersion = configuration.klibAbiVersionForManifest(),
                metadataVersion = configuration.klibMetadataVersionOrDefault(),
            ),
            target = testServices.nativeEnvironmentConfigurator.getNativeTarget(module),
            output = outputKlibArtifactFile.path,
            moduleName = configuration.getNotNull(CommonConfigurationKeys.MODULE_NAME),
            nopack = true,
            shortName = null,
            manifestProperties = null,
        )
    }

    protected abstract fun serialize(
        configuration: CompilerConfiguration,
        usedLibrariesForManifest: List<KotlinLibrary>,
        module: TestModule,
        inputArtifact: IrBackendInput.NativeAfterFrontendBackendInput,
        diagnosticReporter: BaseDiagnosticsCollector,
    ): SerializerOutput<KotlinLibrary>

    private fun updateTestConfiguration(
        configuration: CompilerConfiguration,
        module: TestModule,
        inputArtifact: IrBackendInput.NativeAfterFrontendBackendInput,
        outputArtifact: BinaryArtifacts.KLib
    ) {
        val nativeFactories = KlibMetadataFactories(::KonanBuiltIns, NullFlexibleTypeDeserializer)

        val dependencyPaths = getAllNativeDependenciesPaths(module, testServices)

        val library = resolveLibraries(
            configuration, dependencyPaths + outputArtifact.outputFile.path, knownIrProviders = listOf(KLIB_INTEROP_IR_PROVIDER_IDENTIFIER),
        ).last().library

        val moduleDescriptor = nativeFactories.DefaultDeserializedDescriptorFactory.createDescriptorOptionalBuiltIns(
            library,
            configuration.languageVersionSettings,
            LockBasedStorageManager("ModulesStructure"),
            inputArtifact.irModuleFragment.descriptor.builtIns,
            packageAccessHandler = null,
            lookupTracker = LookupTracker.DO_NOTHING
        )
        moduleDescriptor.setDependencies(dependencyPaths.map { testServices.libraryProvider.getDescriptorByPath(it) as ModuleDescriptorImpl } + moduleDescriptor)

        testServices.moduleDescriptorProvider.replaceModuleDescriptorForModule(module, moduleDescriptor)
        testServices.libraryProvider.setDescriptorAndLibraryByName(outputArtifact.outputFile.path, moduleDescriptor, library)
    }
}

/**
 * The Native KLIB facade suitable for the classic frontend.
 */
class ClassicNativeKlibSerializerFacade(testServices: TestServices) : AbstractNativeKlibSerializerFacade(testServices) {
    override val additionalServices: List<ServiceRegistrationData>
        get() = listOf(service(::LibraryProvider), service(::ModuleDescriptorProvider))

    override fun serialize(
        configuration: CompilerConfiguration,
        usedLibrariesForManifest: List<KotlinLibrary>,
        module: TestModule,
        inputArtifact: IrBackendInput.NativeAfterFrontendBackendInput,
        diagnosticReporter: BaseDiagnosticsCollector,
    ): SerializerOutput<KotlinLibrary> {
        testServices.assertions.assertTrue(inputArtifact.metadataSerializer == null) { "unexpected single-file metadata serializer" }

        val frontendOutput = testServices.artifactsProvider.getArtifact(module, FrontendKinds.ClassicFrontend)

        val serializedMetadata = KlibMetadataMonolithicSerializer(
            configuration.languageVersionSettings,
            metadataVersion = configuration.klibMetadataVersionOrDefault(),
            frontendOutput.project,
            exportKDoc = false,
            skipExpects = true,
        ).serializeModule(frontendOutput.analysisResult.moduleDescriptor)

        val serializerIr = KonanIrModuleSerializer(
            settings = IrSerializationSettings(configuration),
            KtDiagnosticReporterWithImplicitIrBasedContext(diagnosticReporter, configuration.languageVersionSettings),
            inputArtifact.irPluginContext.irBuiltIns,
        ).serializedIrModule(inputArtifact.irModuleFragment)

        return SerializerOutput(
            serializedMetadata,
            serializerIr,
            neededLibraries = usedLibrariesForManifest,
        )
    }
}

/**
 * The Native KLIB facade suitable for FIR frontend.
 */
class FirNativeKlibSerializerFacade(testServices: TestServices) : AbstractNativeKlibSerializerFacade(testServices) {
    override fun serialize(
        configuration: CompilerConfiguration,
        usedLibrariesForManifest: List<KotlinLibrary>,
        module: TestModule,
        inputArtifact: IrBackendInput.NativeAfterFrontendBackendInput,
        diagnosticReporter: BaseDiagnosticsCollector,
    ) = serializeModuleIntoKlib(
        moduleName = inputArtifact.irModuleFragment.name.asString(),
        inputArtifact.irModuleFragment,
        configuration,
        diagnosticReporter,
        cleanFiles = emptyList(),
        usedLibrariesForManifest,
        createModuleSerializer = { irDiagnosticReporter ->
            KonanIrModuleSerializer(
                settings = IrSerializationSettings(configuration),
                diagnosticReporter = irDiagnosticReporter,
                irBuiltIns = inputArtifact.irPluginContext.irBuiltIns,
            )
        },
        inputArtifact.metadataSerializer ?: error("expected metadata serializer"),
    )
}
