/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.converters

import org.jetbrains.kotlin.backend.common.serialization.IdSignatureClashTracker
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.fir.backend.jvm.FirJsSerializerExtension
import org.jetbrains.kotlin.fir.descriptors.FirModuleDescriptor
import org.jetbrains.kotlin.fir.serialization.FirElementSerializer
import org.jetbrains.kotlin.fir.types.typeApproximator
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.ir.backend.js.*
import org.jetbrains.kotlin.ir.util.IrMessageLogger
import org.jetbrains.kotlin.js.config.ErrorTolerancePolicy
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.test.utils.JsIrIncrementalDataProvider
import org.jetbrains.kotlin.js.test.utils.jsIrIncrementalDataProvider
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.test.backend.ir.IrBackendFacade
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.frontend.classic.moduleDescriptorProvider
import org.jetbrains.kotlin.test.frontend.fir.getAllJsDependenciesNames
import org.jetbrains.kotlin.test.frontend.fir.resolveJsLibraries
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.io.File

class JsKlibBackendFacade(
    testServices: TestServices,
    private val firstTimeCompilation: Boolean
) : IrBackendFacade<BinaryArtifacts.KLib>(testServices, ArtifactKinds.KLib) {
    override val additionalServices: List<ServiceRegistrationData>
        get() = listOf(service(::JsIrIncrementalDataProvider))

    constructor(testServices: TestServices): this(testServices, firstTimeCompilation = true)

    override fun shouldRunAnalysis(module: TestModule): Boolean {
        return module.backendKind == inputKind
    }

    override fun transform(module: TestModule, inputArtifact: IrBackendInput): BinaryArtifacts.KLib = when (module.frontendKind) {
        FrontendKinds.ClassicFrontend -> transformAsClassicFrontend(module, inputArtifact)
        FrontendKinds.FIR -> transformAsFir(module, inputArtifact)
        else -> testServices.assertions.fail { "Target frontend ${module.frontendKind} not supported for transformation into KLib" }
    }

    private fun transformAsClassicFrontend(module: TestModule, inputArtifact: IrBackendInput): BinaryArtifacts.KLib {
        require(inputArtifact is IrBackendInput.JsIrBackendInput) {
            "JsKlibBackendFacade expects IrBackendInput.JsIrBackendInput as input"
        }

        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
        val outputFile = JsEnvironmentConfigurator.getJsKlibArtifactPath(testServices, module.name)

        if (firstTimeCompilation) {
            val project = testServices.compilerConfigurationProvider.getProject(module)
            val errorPolicy = configuration.get(JSConfigurationKeys.ERROR_TOLERANCE_POLICY) ?: ErrorTolerancePolicy.DEFAULT
            val hasErrors = TopDownAnalyzerFacadeForJSIR.checkForErrors(inputArtifact.sourceFiles, inputArtifact.bindingContext, errorPolicy)

            serializeModuleIntoKlib(
                configuration[CommonConfigurationKeys.MODULE_NAME]!!,
                project,
                configuration,
                configuration.get(IrMessageLogger.IR_MESSAGE_LOGGER) ?: IrMessageLogger.None,
                inputArtifact.bindingContext,
                inputArtifact.sourceFiles,
                klibPath = outputFile,
                JsEnvironmentConfigurator.getAllRecursiveLibrariesFor(module, testServices).keys.toList(),
                inputArtifact.irModuleFragment,
                inputArtifact.expectDescriptorToSymbol,
                cleanFiles = inputArtifact.icData,
                nopack = true,
                perFile = false,
                containsErrorCode = hasErrors,
                abiVersion = KotlinAbiVersion.CURRENT, // TODO get from test file data
                jsOutputName = null
            )
        }

        val dependencies = JsEnvironmentConfigurator.getAllRecursiveDependenciesFor(module, testServices).toList()
        val lib = jsResolveLibraries(
            dependencies.map { testServices.jsLibraryProvider.getPathByDescriptor(it) } + listOf(outputFile),
            configuration[JSConfigurationKeys.REPOSITORIES] ?: emptyList(),
            configuration[IrMessageLogger.IR_MESSAGE_LOGGER].toResolverLogger()
        ).getFullResolvedList().last().library

        val moduleDescriptor = JsFactories.DefaultDeserializedDescriptorFactory.createDescriptorOptionalBuiltIns(
            lib,
            configuration.languageVersionSettings,
            LockBasedStorageManager("ModulesStructure"),
            testServices.moduleDescriptorProvider.getModuleDescriptor(module).builtIns,
            packageAccessHandler = null,
            lookupTracker = LookupTracker.DO_NOTHING
        )
        moduleDescriptor.setDependencies(dependencies + moduleDescriptor)
        testServices.moduleDescriptorProvider.replaceModuleDescriptorForModule(module, moduleDescriptor)

        if (JsEnvironmentConfigurator.incrementalEnabled(testServices)) {
            testServices.jsIrIncrementalDataProvider.recordIncrementalData(module, lib)
        }
        testServices.jsLibraryProvider.setDescriptorAndLibraryByName(outputFile, moduleDescriptor, lib)

        return BinaryArtifacts.KLib(File(outputFile))
    }

    private fun transformAsFir(module: TestModule, inputArtifact: IrBackendInput): BinaryArtifacts.KLib {
        require(inputArtifact is IrBackendInput.JsIrBackendInput) {
            "JsKlibBackendFacade expects IrBackendInput.JsIrBackendInput as input"
        }

        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
        val outputFile = JsEnvironmentConfigurator.getJsKlibArtifactPath(testServices, module.name)

        val libraries = resolveJsLibraries(module, testServices, configuration)

        val descriptor = inputArtifact.irModuleFragment.descriptor.safeAs<FirModuleDescriptor>()
            ?: throw Exception("Expected a FirModuleDescriptor")

        val scopeSession = inputArtifact.scopeSession
            ?: throw Exception("No ScopeSession for FIR")

        val components = inputArtifact.components
            ?: throw Exception("No Fir2IrComponents for FIR")

        val serializer = FirElementSerializer.createTopLevel(
            descriptor.session,
            scopeSession,
            FirJsSerializerExtension(descriptor.session, components),
            descriptor.session.typeApproximator,
        )

        if (firstTimeCompilation) {
            val project = testServices.compilerConfigurationProvider.getProject(module)
            val errorPolicy = configuration.get(JSConfigurationKeys.ERROR_TOLERANCE_POLICY) ?: ErrorTolerancePolicy.DEFAULT
            val hasErrors = TopDownAnalyzerFacadeForJSIR.checkForErrors(inputArtifact.sourceFiles, inputArtifact.bindingContext, errorPolicy)

            serializeModuleIntoKlibAsFir(
                configuration[CommonConfigurationKeys.MODULE_NAME]!!,
                project,
                configuration,
                configuration.get(IrMessageLogger.IR_MESSAGE_LOGGER) ?: IrMessageLogger.None,
                inputArtifact.bindingContext,
                inputArtifact.sourceFiles,
                klibPath = outputFile,
                libraries.map { it.library },
                inputArtifact.irModuleFragment,
                inputArtifact.expectDescriptorToSymbol,
                cleanFiles = inputArtifact.icData,
                nopack = true,
                perFile = false,
                containsErrorCode = hasErrors,
                abiVersion = KotlinAbiVersion.CURRENT, // TODO get from test file data
                jsOutputName = null,
                session = descriptor.session,
                serializer = serializer,
                signatureTracker = IdSignatureClashTracker.DEFAULT_TRACKER,
                firFiles = inputArtifact.firFiles,
            )
        }

        // The resolution works like this:
        // it resolves libraries by absolute paths
        // and associates the results with short names
        // used in dependencies lists of subsequent libraries.
        // TODO: preserve old resolution results
        val librariesWithThis = getAllJsDependenciesNames(module, testServices) + outputFile
        val repositories = configuration[JSConfigurationKeys.REPOSITORIES] ?: emptyList()
        val logger = configuration[IrMessageLogger.IR_MESSAGE_LOGGER].toResolverLogger()
        val lib = jsResolveLibraries(librariesWithThis, repositories, logger).getFullResolvedList().last().library

        if (JsEnvironmentConfigurator.incrementalEnabled(testServices)) {
            testServices.jsIrIncrementalDataProvider.recordIncrementalData(module, lib)
        }

        return BinaryArtifacts.KLib(File(outputFile))
    }
}