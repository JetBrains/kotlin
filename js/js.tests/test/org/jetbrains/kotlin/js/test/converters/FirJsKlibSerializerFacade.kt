/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.converters

import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.ir.backend.js.JsFactories
import org.jetbrains.kotlin.ir.backend.js.loadWebKlibsInTestPipeline
import org.jetbrains.kotlin.ir.backend.js.serializeModuleIntoKlib
import org.jetbrains.kotlin.js.test.utils.JsIrIncrementalDataProvider
import org.jetbrains.kotlin.js.test.utils.jsIrIncrementalDataProvider
import org.jetbrains.kotlin.library.loader.KlibPlatformChecker
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.test.backend.ir.IrBackendFacade
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.directives.KlibBasedCompilerTestDirectives.SKIP_GENERATING_KLIB
import org.jetbrains.kotlin.test.frontend.classic.ModuleDescriptorProvider
import org.jetbrains.kotlin.test.frontend.classic.moduleDescriptorProvider
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.getDependencies
import org.jetbrains.kotlin.test.services.configuration.getFriendDependencies

class FirJsKlibSerializerFacade(
    testServices: TestServices,
    private val firstTimeCompilation: Boolean
) : IrBackendFacade<BinaryArtifacts.KLib>(testServices, ArtifactKinds.KLib) {

    override val additionalServices: List<ServiceRegistrationData>
        get() = listOf(service(::JsIrIncrementalDataProvider), service(::ModuleDescriptorProvider))

    constructor(testServices: TestServices) : this(testServices, firstTimeCompilation = true)

    override fun shouldTransform(module: TestModule): Boolean {
        return testServices.defaultsProvider.backendKind == inputKind && SKIP_GENERATING_KLIB !in module.directives
    }

    override fun transform(module: TestModule, inputArtifact: IrBackendInput): BinaryArtifacts.KLib {
        require(inputArtifact is IrBackendInput.JsIrAfterFrontendBackendInput) {
            "FirJsKlibSerializerFacade expects IrBackendInput.JsIrAfterFrontendBackendInput as input"
        }

        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
        val diagnosticReporter = DiagnosticReporterFactory.createReporter(configuration.messageCollector)
        val outputFile = JsEnvironmentConfigurator.getKlibArtifactFile(testServices, module.name)

        if (firstTimeCompilation) {
            serializeModuleIntoKlib(
                moduleName = configuration[CommonConfigurationKeys.MODULE_NAME]!!,
                configuration = configuration,
                diagnosticReporter = diagnosticReporter,
                metadataSerializer = inputArtifact.metadataSerializer,
                klibPath = outputFile.path,
                dependencies = emptyList(), // Does not matter.
                moduleFragment = inputArtifact.irModuleFragment,
                irBuiltIns = inputArtifact.irPluginContext.irBuiltIns,
                cleanFiles = inputArtifact.icData,
                nopack = true,
                containsErrorCode = inputArtifact.hasErrors,
                jsOutputName = null
            )
        }

        val lib = loadWebKlibsInTestPipeline(
            configuration = configuration,
            libraryPaths = listOf(outputFile.path),
            platformChecker = KlibPlatformChecker.JS
        ).all.single()

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

        return BinaryArtifacts.KLib(outputFile, diagnosticReporter)
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
