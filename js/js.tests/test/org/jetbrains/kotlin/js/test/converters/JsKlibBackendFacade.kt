/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.converters

import org.jetbrains.kotlin.backend.common.CommonKLibResolver
import org.jetbrains.kotlin.cli.common.messages.getLogger
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.ir.backend.js.*
import org.jetbrains.kotlin.js.test.utils.JsIrIncrementalDataProvider
import org.jetbrains.kotlin.js.test.utils.jsIrIncrementalDataProvider
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.test.backend.ir.IrBackendFacade
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.frontend.classic.moduleDescriptorProvider
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import java.io.File

class JsKlibBackendFacade(
    testServices: TestServices,
    private val firstTimeCompilation: Boolean
) : IrBackendFacade<BinaryArtifacts.KLib>(testServices, ArtifactKinds.KLib) {
    override val additionalServices: List<ServiceRegistrationData>
        get() = listOf(service(::JsIrIncrementalDataProvider))

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

        if (firstTimeCompilation) {
            serializeModuleIntoKlib(
                configuration[CommonConfigurationKeys.MODULE_NAME]!!,
                configuration,
                inputArtifact.diagnosticReporter,
                inputArtifact.metadataSerializer,
                klibPath = outputFile,
                JsEnvironmentConfigurator.getAllRecursiveLibrariesFor(module, testServices).keys.toList(),
                inputArtifact.irModuleFragment,
                cleanFiles = inputArtifact.icData,
                nopack = true,
                perFile = false,
                containsErrorCode = inputArtifact.hasErrors,
                abiVersion = KotlinAbiVersion.CURRENT, // TODO get from test file data
                jsOutputName = null
            )
        }

        val dependencies = JsEnvironmentConfigurator.getAllRecursiveDependenciesFor(module, testServices).toList()
        val lib = CommonKLibResolver.resolve(
            dependencies.map { testServices.libraryProvider.getPathByDescriptor(it) } + listOf(outputFile),
            configuration.getLogger(treatWarningsAsErrors = true)
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
        testServices.libraryProvider.setDescriptorAndLibraryByName(outputFile, moduleDescriptor, lib)

        return BinaryArtifacts.KLib(File(outputFile), inputArtifact.diagnosticReporter)
    }
}
