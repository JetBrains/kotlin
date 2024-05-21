/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
import org.jetbrains.kotlin.test.services.configuration.getDependencies
import org.jetbrains.kotlin.test.services.configuration.getFriendDependencies

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
        val outputFile = JsEnvironmentConfigurator.getKlibArtifactFile(testServices, module.name)

        // TODO: consider avoiding repeated libraries resolution
        val libraries = resolveLibraries(configuration, getAllJsDependenciesPaths(module, testServices))

        if (firstTimeCompilation) {
            serializeModuleIntoKlib(
                configuration[CommonConfigurationKeys.MODULE_NAME]!!,
                configuration,
                inputArtifact.diagnosticReporter,
                inputArtifact.metadataSerializer,
                klibPath = outputFile.path,
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
            getAllJsDependenciesPaths(module, testServices) + listOf(outputFile.path),
            configuration.getLogger(treatWarningsAsErrors = true)
        ).getFullResolvedList().last().library

        val moduleDescriptor = JsFactories.DefaultDeserializedDescriptorFactory.createDescriptorOptionalBuiltIns(
            lib,
            configuration.languageVersionSettings,
            LockBasedStorageManager("ModulesStructure"),
            inputArtifact.irModuleFragment.descriptor.builtIns,
            packageAccessHandler = null,
            lookupTracker = LookupTracker.DO_NOTHING
        ).apply {
            setDependencies(inputArtifact.regularDependencyModules + this, getFriendDependencies(module, testServices))
        }

        testServices.moduleDescriptorProvider.replaceModuleDescriptorForModule(module, moduleDescriptor)
        if (JsEnvironmentConfigurator.incrementalEnabled(testServices)) {
            testServices.jsIrIncrementalDataProvider.recordIncrementalData(module, lib)
        }
        testServices.libraryProvider.setDescriptorAndLibraryByName(outputFile.path, moduleDescriptor, lib)

        return BinaryArtifacts.KLib(outputFile, inputArtifact.diagnosticReporter)
    }


    /**
     * Note: If it is implemented the same way as [getFriendDependencies] (i.e., get regular
     * dependencies through [getDependencies] call), then important dependencies which do
     * not exist in the form of [TestModule] such as stdlib & stdlib-test would not be included here.
     */
    private val IrBackendInput.regularDependencyModules: List<ModuleDescriptorImpl>
        get() = irModuleFragment.descriptor.allDependencyModules
            .filterIsInstance<ModuleDescriptorImpl>()

}
