/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.converters

import org.jetbrains.kotlin.backend.common.CommonKLibResolver
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.ir.backend.js.JsFactories
import org.jetbrains.kotlin.ir.backend.js.resolverLogger
import org.jetbrains.kotlin.ir.backend.js.serializeModuleIntoKlib
import org.jetbrains.kotlin.ir.util.IrMessageLogger
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.config.WasmTarget
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.test.backend.ir.IrBackendFacade
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.frontend.classic.ModuleDescriptorProvider
import org.jetbrains.kotlin.test.frontend.classic.moduleDescriptorProvider
import org.jetbrains.kotlin.test.frontend.fir.getAllWasmDependenciesPaths
import org.jetbrains.kotlin.test.frontend.fir.resolveLibraries
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.services.configuration.WasmEnvironmentConfigurator
import java.io.File

class FirWasmKlibBackendFacade(
    testServices: TestServices,
    private val firstTimeCompilation: Boolean
) : IrBackendFacade<BinaryArtifacts.KLib>(testServices, ArtifactKinds.KLib) {

    override val additionalServices: List<ServiceRegistrationData>
        get() = listOf(service(::ModuleDescriptorProvider))

    constructor(testServices: TestServices) : this(testServices, firstTimeCompilation = true)

    override fun shouldRunAnalysis(module: TestModule): Boolean {
        return module.backendKind == inputKind
    }

    override fun transform(module: TestModule, inputArtifact: IrBackendInput): BinaryArtifacts.KLib {
        require(inputArtifact is IrBackendInput.WasmBackendInput) {
            "FirWasmKlibBackendFacade expects IrBackendInput.WasmBackendInput as input"
        }

        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
        val outputFile = WasmEnvironmentConfigurator.getWasmKlibArtifactPath(testServices, module.name)

        // TODO: consider avoiding repeated libraries resolution
        val target = configuration.get(JSConfigurationKeys.WASM_TARGET, WasmTarget.JS)
        val libraries = resolveLibraries(configuration, getAllWasmDependenciesPaths(module, testServices, target))

        if (firstTimeCompilation) {
            serializeModuleIntoKlib(
                configuration[CommonConfigurationKeys.MODULE_NAME]!!,
                configuration,
                configuration.get(IrMessageLogger.IR_MESSAGE_LOGGER) ?: IrMessageLogger.None,
                inputArtifact.sourceFiles,
                klibPath = outputFile,
                libraries.map { it.library },
                inputArtifact.irModuleFragment,
                cleanFiles = inputArtifact.icData,
                nopack = true,
                perFile = false,
                containsErrorCode = inputArtifact.hasErrors,
                abiVersion = KotlinAbiVersion.CURRENT, // TODO get from test file data
                jsOutputName = null
            ) {
                inputArtifact.serializeSingleFile(it, inputArtifact.irActualizerResult)
            }
        }

        // TODO: consider avoiding repeated libraries resolution
        val lib = CommonKLibResolver.resolve(
            getAllWasmDependenciesPaths(module, testServices, target) + listOf(outputFile),
            configuration.resolverLogger
        ).getFullResolvedList().last().library

        val moduleDescriptor = JsFactories.DefaultDeserializedDescriptorFactory.createDescriptorOptionalBuiltIns(
            lib,
            configuration.languageVersionSettings,
            LockBasedStorageManager("ModulesStructure"),
            inputArtifact.irModuleFragment.descriptor.builtIns,
            packageAccessHandler = null,
            lookupTracker = LookupTracker.DO_NOTHING
        )

        moduleDescriptor.setDependencies(
            inputArtifact.irModuleFragment.descriptor.allDependencyModules.filterIsInstance<ModuleDescriptorImpl>() + moduleDescriptor
        )

        testServices.moduleDescriptorProvider.replaceModuleDescriptorForModule(module, moduleDescriptor)
        testServices.libraryProvider.setDescriptorAndLibraryByName(outputFile, moduleDescriptor, lib)

        return BinaryArtifacts.KLib(File(outputFile))
    }
}
