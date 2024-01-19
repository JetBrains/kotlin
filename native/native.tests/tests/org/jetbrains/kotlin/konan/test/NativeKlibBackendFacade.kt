/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test

import org.jetbrains.kotlin.KtDiagnosticReporterWithImplicitIrBasedContext
import org.jetbrains.kotlin.backend.common.CommonKLibResolver
import org.jetbrains.kotlin.backend.common.serialization.CompatibilityMode
import org.jetbrains.kotlin.backend.konan.serialization.KonanIrModuleSerializer
import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.cli.common.messages.getLogger
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.ir.util.irMessageLogger
import org.jetbrains.kotlin.konan.library.impl.buildLibrary
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.library.KLIB_PROPERTY_CONTAINS_ERROR_CODE
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.library.KotlinLibraryVersioning
import org.jetbrains.kotlin.library.SerializedIrModule
import org.jetbrains.kotlin.library.metadata.KlibMetadataFactories
import org.jetbrains.kotlin.library.metadata.NullFlexibleTypeDeserializer
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.test.backend.ir.IrBackendFacade
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.frontend.classic.moduleDescriptorProvider
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import org.jetbrains.kotlin.test.services.configuration.NativeEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.libraryProvider
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.metadataVersion
import java.util.*

/**
 * The unified Native KLIB facade suitable both for K1 and K2 frontends.
 */
class NativeKlibBackendFacade(
    testServices: TestServices
) : IrBackendFacade<BinaryArtifacts.KLib>(testServices, ArtifactKinds.KLib) {
    override fun shouldRunAnalysis(module: TestModule): Boolean {
        return module.backendKind == inputKind
    }

    override fun transform(module: TestModule, inputArtifact: IrBackendInput): BinaryArtifacts.KLib {
        require(inputArtifact is IrBackendInput.NativeBackendInput) {
            "NativeKlibBackendFacade expects IrBackendInput.NativeBackendInput as input"
        }

        val outputArtifact = serializeToNativeKlib(module, inputArtifact)
        updateTestConfiguration(module, outputArtifact)

        return outputArtifact
    }

    private fun serializeToNativeKlib(module: TestModule, inputArtifact: IrBackendInput.NativeBackendInput): BinaryArtifacts.KLib {
        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
        val klibFile = NativeEnvironmentConfigurator.getKlibArtifactFile(testServices, module.name)

        buildLibrary(
            natives = emptyList(),
            included = emptyList(),
            linkDependencies = NativeEnvironmentConfigurator.getAllRecursiveLibrariesFor(module, testServices).keys.toList(),
            metadata = inputArtifact.serializeMetadata(),
            ir = inputArtifact.serializeIr(configuration),
            versions = KotlinLibraryVersioning(
                abiVersion = KotlinAbiVersion.CURRENT,
                libraryVersion = null,
                compilerVersion = KotlinCompilerVersion.getVersion(),
                metadataVersion = configuration.metadataVersion().toString(),
            ),
            target = HostManager.host,
            output = klibFile.path,
            moduleName = configuration.getNotNull(CommonConfigurationKeys.MODULE_NAME),
            nopack = true,
            shortName = null,
            manifestProperties = runIf(inputArtifact.hasErrors) {
                Properties().apply { this[KLIB_PROPERTY_CONTAINS_ERROR_CODE] = "true" }
            },
            dataFlowGraph = null
        )

        return BinaryArtifacts.KLib(klibFile, inputArtifact.diagnosticReporter)
    }

    private fun updateTestConfiguration(module: TestModule, outputArtifact: BinaryArtifacts.KLib) {
        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
        val nativeFactories = KlibMetadataFactories(::KonanBuiltIns, NullFlexibleTypeDeserializer)

        val dependencies = NativeEnvironmentConfigurator.getAllRecursiveDependenciesFor(module, testServices).toList()
        val library = CommonKLibResolver.resolve(
            dependencies.map { testServices.libraryProvider.getPathByDescriptor(it) } + outputArtifact.outputFile.path,
            configuration.getLogger(treatWarningsAsErrors = true)
        ).getFullResolvedList().last().library

        val moduleDescriptor = nativeFactories.DefaultDeserializedDescriptorFactory.createDescriptorOptionalBuiltIns(
            library,
            configuration.languageVersionSettings,
            LockBasedStorageManager("ModulesStructure"),
            testServices.moduleDescriptorProvider.getModuleDescriptor(module).builtIns,
            packageAccessHandler = null,
            lookupTracker = LookupTracker.DO_NOTHING
        )
        moduleDescriptor.setDependencies(dependencies + moduleDescriptor)
        testServices.moduleDescriptorProvider.replaceModuleDescriptorForModule(module, moduleDescriptor)
        testServices.libraryProvider.setDescriptorAndLibraryByName(outputArtifact.outputFile.path, moduleDescriptor, library)
    }

    companion object {
        private fun IrBackendInput.NativeBackendInput.serializeIr(configuration: CompilerConfiguration): SerializedIrModule {
            val irSerializer = KonanIrModuleSerializer(
                KtDiagnosticReporterWithImplicitIrBasedContext(diagnosticReporter, configuration.languageVersionSettings),
                configuration.irMessageLogger,
                irModuleFragment.irBuiltins,
                CompatibilityMode.CURRENT,
                normalizeAbsolutePaths = configuration[CommonConfigurationKeys.KLIB_NORMALIZE_ABSOLUTE_PATH] ?: false,
                sourceBaseDirs = configuration[CommonConfigurationKeys.KLIB_RELATIVE_PATH_BASES].orEmpty(),
                configuration.languageVersionSettings,
                shouldCheckSignaturesOnUniqueness = configuration[CommonConfigurationKeys.PRODUCE_KLIB_SIGNATURES_CLASH_CHECKS] ?: true
            )
            return irSerializer.serializedIrModule(irModuleFragment)
        }
    }
}
