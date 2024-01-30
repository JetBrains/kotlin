/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.converters

import org.jetbrains.kotlin.backend.common.CommonKLibResolver
import org.jetbrains.kotlin.cli.common.messages.getLogger
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.ir.backend.js.JsFactories
import org.jetbrains.kotlin.ir.backend.js.serializeModuleIntoKlib
import org.jetbrains.kotlin.js.test.utils.JsIrIncrementalDataProvider
import org.jetbrains.kotlin.js.test.utils.jsIrIncrementalDataProvider
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.test.backend.ir.IrBackendFacade
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.frontend.classic.ModuleDescriptorProvider
import org.jetbrains.kotlin.test.frontend.classic.moduleDescriptorProvider
import org.jetbrains.kotlin.test.frontend.fir.getAllJsDependenciesPaths
import org.jetbrains.kotlin.test.frontend.fir.resolveLibraries
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.io.File

class FirJsKlibBackendFacade(
    testServices: TestServices,
    private val firstTimeCompilation: Boolean
) : IrBackendFacade<BinaryArtifacts.KLib>(testServices, ArtifactKinds.KLib) {

    override val additionalServices: List<ServiceRegistrationData>
        get() = listOf(service(::JsIrIncrementalDataProvider), service(::ModuleDescriptorProvider))

    constructor(testServices: TestServices) : this(testServices, firstTimeCompilation = true)

    override fun shouldRunAnalysis(module: TestModule): Boolean {
        return module.backendKind == inputKind
    }

    override fun transform(module: TestModule, inputArtifact: IrBackendInput): BinaryArtifacts.KLib {
        require(inputArtifact is IrBackendInput.JsIrBackendInput) {
            "JsKlibBackendFacade expects IrBackendInput.JsIrBackendInput as input"
        }

        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
        val outputFile = JsEnvironmentConfigurator.getJsKlibArtifactPath(testServices, module.name)

        // TODO: consider avoiding repeated libraries resolution
        val libraries = resolveLibraries(configuration, getAllJsDependenciesPaths(module, testServices))

        if (firstTimeCompilation) {
            serializeModuleIntoKlib(
                configuration[CommonConfigurationKeys.MODULE_NAME]!!,
                configuration,
                inputArtifact.diagnosticReporter,
                inputArtifact.metadataSerializer,
                klibPath = outputFile,
                libraries.map { it.library },
                inputArtifact.irModuleFragment,
                cleanFiles = inputArtifact.icData,
                nopack = true,
                perFile = false,
                containsErrorCode = inputArtifact.hasErrors,
                abiVersion = KotlinAbiVersion.CURRENT, // TODO get from test file data
                jsOutputName = null
            )
        }

        // TODO: consider avoiding repeated libraries resolution
        val lib = CommonKLibResolver.resolve(
            getAllJsDependenciesPaths(module, testServices) + listOf(outputFile),
            configuration.getLogger(treatWarningsAsErrors = true)
        ).getFullResolvedList().last().library

        val moduleDescriptor = JsFactories.DefaultDeserializedDescriptorFactory.createDescriptorOptionalBuiltIns(
            lib,
            configuration.languageVersionSettings,
            LockBasedStorageManager("ModulesStructure"),
            inputArtifact.irModuleFragment.descriptor.builtIns,
            packageAccessHandler = null,
            lookupTracker = LookupTracker.DO_NOTHING
        )
        // TODO: find out why it must be so weird
        moduleDescriptor.safeAs<ModuleDescriptorImpl>()?.let {
            it.setDependencies(inputArtifact.irModuleFragment.descriptor.allDependencyModules.filterIsInstance<ModuleDescriptorImpl>() + it)
        }

        testServices.moduleDescriptorProvider.replaceModuleDescriptorForModule(module, moduleDescriptor)
        if (JsEnvironmentConfigurator.incrementalEnabled(testServices)) {
            testServices.jsIrIncrementalDataProvider.recordIncrementalData(module, lib)
        }
        testServices.libraryProvider.setDescriptorAndLibraryByName(outputFile, moduleDescriptor, lib)

        return BinaryArtifacts.KLib(File(outputFile), inputArtifact.diagnosticReporter)
    }
}
