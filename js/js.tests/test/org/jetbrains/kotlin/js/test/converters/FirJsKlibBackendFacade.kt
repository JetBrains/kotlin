/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.converters

import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.ir.backend.js.jsResolveLibraries
import org.jetbrains.kotlin.ir.backend.js.serializeModuleIntoKlib
import org.jetbrains.kotlin.ir.backend.js.toResolverLogger
import org.jetbrains.kotlin.ir.util.IrMessageLogger
import org.jetbrains.kotlin.js.test.utils.JsIrIncrementalDataProvider
import org.jetbrains.kotlin.js.test.utils.jsIrIncrementalDataProvider
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.test.backend.ir.IrBackendFacade
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.frontend.classic.ModuleDescriptorProvider
import org.jetbrains.kotlin.test.frontend.classic.moduleDescriptorProvider
import org.jetbrains.kotlin.test.frontend.fir.getAllJsDependenciesPaths
import org.jetbrains.kotlin.test.frontend.fir.resolveJsLibraries
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import java.io.File

class FirJsKlibBackendFacade(
    testServices: TestServices,
    private val firstTimeCompilation: Boolean
) : IrBackendFacade<BinaryArtifacts.KLib>(testServices, ArtifactKinds.KLib)  {

    override val additionalServices: List<ServiceRegistrationData>
        get() = listOf(service(::JsIrIncrementalDataProvider), service(::ModuleDescriptorProvider))

    constructor(testServices: TestServices): this(testServices, firstTimeCompilation = true)

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
        val libraries = resolveJsLibraries(module, testServices, configuration)

        if (firstTimeCompilation) {
            serializeModuleIntoKlib(
                configuration[CommonConfigurationKeys.MODULE_NAME]!!,
                configuration,
                configuration.get(IrMessageLogger.IR_MESSAGE_LOGGER) ?: IrMessageLogger.None,
                inputArtifact.sourceFiles,
                klibPath = outputFile,
                libraries.map { it.library },
                inputArtifact.irModuleFragment,
                inputArtifact.expectDescriptorToSymbol,
                cleanFiles = inputArtifact.icData,
                nopack = true,
                perFile = false,
                containsErrorCode = inputArtifact.hasErrors,
                abiVersion = KotlinAbiVersion.CURRENT, // TODO get from test file data
                jsOutputName = null,
                inputArtifact.serializeSingleFile
            )
        }

        // TODO: consider avoiding repeated libraries resolution
        val lib = jsResolveLibraries(
            getAllJsDependenciesPaths(module, testServices) + listOf(outputFile),
            emptyList(),
            configuration[IrMessageLogger.IR_MESSAGE_LOGGER].toResolverLogger()
        ).getFullResolvedList().last().library

        testServices.moduleDescriptorProvider.replaceModuleDescriptorForModule(module, inputArtifact.irModuleFragment.descriptor)
        if (JsEnvironmentConfigurator.incrementalEnabled(testServices)) {
            testServices.jsIrIncrementalDataProvider.recordIncrementalData(module, lib)
        }
        testServices.jsLibraryProvider.setDescriptorAndLibraryByName(outputFile, inputArtifact.irModuleFragment.descriptor, lib)

        return BinaryArtifacts.KLib(File(outputFile))
    }
}