/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test

import org.jetbrains.kotlin.backend.common.serialization.CompatibilityMode
import org.jetbrains.kotlin.backend.common.serialization.SerializerOutput
import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibMetadataMonolithicSerializer
import org.jetbrains.kotlin.backend.common.serialization.serializeModuleIntoKlib
import org.jetbrains.kotlin.backend.konan.serialization.KonanIrModuleSerializer
import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.KlibConfigurationKeys
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.ir.KtDiagnosticReporterWithImplicitIrBasedContext
import org.jetbrains.kotlin.konan.library.KLIB_INTEROP_IR_PROVIDER_IDENTIFIER
import org.jetbrains.kotlin.konan.library.impl.buildLibrary
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.library.metadata.KlibMetadataFactories
import org.jetbrains.kotlin.library.metadata.KlibMetadataVersion
import org.jetbrains.kotlin.library.metadata.NullFlexibleTypeDeserializer
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.test.backend.ir.IrBackendFacade
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.frontend.classic.ModuleDescriptorProvider
import org.jetbrains.kotlin.test.frontend.classic.moduleDescriptorProvider
import org.jetbrains.kotlin.test.frontend.fir.getAllNativeDependenciesPaths
import org.jetbrains.kotlin.test.frontend.fir.resolveLibraries
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.services.configuration.NativeEnvironmentConfigurator.Companion.getKlibArtifactFile
import org.jetbrains.kotlin.utils.metadataVersion

abstract class AbstractNativeKlibBackendFacade(
    testServices: TestServices
) : IrBackendFacade<BinaryArtifacts.KLib>(testServices, ArtifactKinds.KLib) {
    final override fun shouldRunAnalysis(module: TestModule): Boolean {
        return module.backendKind == inputKind
    }

    final override fun transform(module: TestModule, inputArtifact: IrBackendInput): BinaryArtifacts.KLib {
        require(inputArtifact is IrBackendInput.NativeBackendInput) {
            "${this::class.java.simpleName} expects IrBackendInput.NativeBackendInput as input"
        }

        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)

        val dependencyPaths = getAllNativeDependenciesPaths(module, testServices)
        val dependencies = resolveLibraries(configuration, dependencyPaths, knownIrProviders = listOf(KLIB_INTEROP_IR_PROVIDER_IDENTIFIER))
            .map { it.library }

        val serializerOutput = serialize(configuration, dependencies, module, inputArtifact)

        val outputArtifact = BinaryArtifacts.KLib(getKlibArtifactFile(testServices, module.name), inputArtifact.diagnosticReporter)

        buildLibrary(
            natives = emptyList(),
            included = emptyList(),
            linkDependencies = serializerOutput.neededLibraries,
            serializerOutput.serializedMetadata ?: testServices.assertions.fail { "expected serialized metadata" },
            serializerOutput.serializedIr,
            versions = KotlinLibraryVersioning(
                abiVersion = KotlinAbiVersion.CURRENT,
                compilerVersion = KotlinCompilerVersion.getVersion(),
                metadataVersion = configuration.metadataVersion().toString(),
            ),
            target = HostManager.host,
            output = outputArtifact.outputFile.path,
            moduleName = configuration.getNotNull(CommonConfigurationKeys.MODULE_NAME),
            nopack = true,
            shortName = null,
            manifestProperties = null,
            dataFlowGraph = null
        )

        updateTestConfiguration(configuration, dependencyPaths, module, outputArtifact)

        return outputArtifact
    }

    protected abstract fun serialize(
        configuration: CompilerConfiguration,
        dependencies: List<KotlinLibrary>,
        module: TestModule,
        inputArtifact: IrBackendInput.NativeBackendInput,
    ): SerializerOutput<KotlinLibrary>

    protected open fun updateTestConfiguration(
        configuration: CompilerConfiguration,
        dependencyPaths: List<String>,
        module: TestModule,
        outputArtifact: BinaryArtifacts.KLib
    ) = Unit
}

/**
 * The Native KLIB facade suitable for the classic frontend.
 */
class ClassicNativeKlibBackendFacade(testServices: TestServices) : AbstractNativeKlibBackendFacade(testServices) {
    override val additionalServices: List<ServiceRegistrationData>
        get() = listOf(service(::LibraryProvider), service(::ModuleDescriptorProvider))

    override fun serialize(
        configuration: CompilerConfiguration,
        dependencies: List<KotlinLibrary>,
        module: TestModule,
        inputArtifact: IrBackendInput.NativeBackendInput,
    ): SerializerOutput<KotlinLibrary> {
        testServices.assertions.assertTrue(inputArtifact.firMangler == null) { "unexpected Fir mangler" }
        testServices.assertions.assertTrue(inputArtifact.metadataSerializer == null) { "unexpected single-file metadata serializer" }

        val frontendOutput = testServices.dependencyProvider.getArtifact(module, FrontendKinds.ClassicFrontend)

        val serializedMetadata = KlibMetadataMonolithicSerializer(
            configuration.languageVersionSettings,
            metadataVersion = configuration[CommonConfigurationKeys.METADATA_VERSION] as? KlibMetadataVersion
                ?: KlibMetadataVersion.INSTANCE,
            frontendOutput.project,
            exportKDoc = false,
            skipExpects = true,
            allowErrorTypes = false,
        ).serializeModule(frontendOutput.analysisResult.moduleDescriptor)

        val serializerIr = KonanIrModuleSerializer(
            KtDiagnosticReporterWithImplicitIrBasedContext(inputArtifact.diagnosticReporter, configuration.languageVersionSettings),
            inputArtifact.irModuleFragment.irBuiltins,
            CompatibilityMode.CURRENT,
            normalizeAbsolutePaths = configuration.getBoolean(KlibConfigurationKeys.KLIB_NORMALIZE_ABSOLUTE_PATH),
            sourceBaseDirs = configuration.getList(KlibConfigurationKeys.KLIB_RELATIVE_PATH_BASES),
            configuration.languageVersionSettings,
            shouldCheckSignaturesOnUniqueness = configuration.get(KlibConfigurationKeys.PRODUCE_KLIB_SIGNATURES_CLASH_CHECKS, true)
        ).serializedIrModule(inputArtifact.irModuleFragment)

        return SerializerOutput(
            serializedMetadata,
            serializerIr,
            dataFlowGraph = null,
            neededLibraries = dependencies
        )
    }

    override fun updateTestConfiguration(
        configuration: CompilerConfiguration,
        dependencyPaths: List<String>,
        module: TestModule,
        outputArtifact: BinaryArtifacts.KLib
    ) {
        val nativeFactories = KlibMetadataFactories(::KonanBuiltIns, NullFlexibleTypeDeserializer)

        val library = resolveLibraries(
            configuration, dependencyPaths + outputArtifact.outputFile.path,
        ).last().library

        val moduleDescriptor = nativeFactories.DefaultDeserializedDescriptorFactory.createDescriptorOptionalBuiltIns(
            library,
            configuration.languageVersionSettings,
            LockBasedStorageManager("ModulesStructure"),
            testServices.moduleDescriptorProvider.getModuleDescriptor(module).builtIns,
            packageAccessHandler = null,
            lookupTracker = LookupTracker.DO_NOTHING
        )
        moduleDescriptor.setDependencies(dependencyPaths.map { testServices.libraryProvider.getDescriptorByPath(it) as ModuleDescriptorImpl } + moduleDescriptor)

        testServices.moduleDescriptorProvider.replaceModuleDescriptorForModule(module, moduleDescriptor)
        testServices.libraryProvider.setDescriptorAndLibraryByName(outputArtifact.outputFile.path, moduleDescriptor, library)
    }
}

/**
 * The Native KLIB facade suitable for FIR frontend.
 */
class FirNativeKlibBackendFacade(testServices: TestServices) : AbstractNativeKlibBackendFacade(testServices) {
    override fun serialize(
        configuration: CompilerConfiguration,
        dependencies: List<KotlinLibrary>,
        module: TestModule,
        inputArtifact: IrBackendInput.NativeBackendInput,
    ) = serializeModuleIntoKlib(
        moduleName = inputArtifact.irModuleFragment.name.asString(),
        inputArtifact.irModuleFragment,
        configuration,
        inputArtifact.diagnosticReporter,
        CompatibilityMode.CURRENT,
        cleanFiles = emptyList(),
        dependencies,
        createModuleSerializer = {
                irDiagnosticReporter,
                irBuiltIns,
                compatibilityMode,
                normalizeAbsolutePaths,
                sourceBaseDirs,
                languageVersionSettings,
                shouldCheckSignaturesOnUniqueness,
            ->
            KonanIrModuleSerializer(
                diagnosticReporter = irDiagnosticReporter,
                irBuiltIns = irBuiltIns,
                compatibilityMode = compatibilityMode,
                normalizeAbsolutePaths = normalizeAbsolutePaths,
                sourceBaseDirs = sourceBaseDirs,
                languageVersionSettings = languageVersionSettings,
                bodiesOnlyForInlines = false,
                publicAbiOnly = false,
                shouldCheckSignaturesOnUniqueness = shouldCheckSignaturesOnUniqueness,
            )
        },
        inputArtifact.metadataSerializer ?: error("expected metadata serializer"),
    )
}
