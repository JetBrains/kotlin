/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.converters

import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.diagnostics.impl.DiagnosticsCollectorImpl
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.ir.KtDiagnosticReporterWithImplicitIrBasedContext
import org.jetbrains.kotlin.ir.backend.js.JsFactories
import org.jetbrains.kotlin.ir.backend.js.loadWebKlibsInTestPipeline
import org.jetbrains.kotlin.ir.backend.js.serializeModuleIntoKlib
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.library.loader.KlibPlatformChecker
import org.jetbrains.kotlin.platform.wasm.WasmTarget
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
import org.jetbrains.kotlin.test.services.configuration.WasmEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.getFriendDependencies
import org.jetbrains.kotlin.test.services.configuration.klibEnvironmentConfigurator
import org.jetbrains.kotlin.wasm.config.WasmConfigurationKeys
import org.jetbrains.kotlin.wasm.config.wasmTarget
import org.jetbrains.kotlin.wasm.test.handlers.WASM_BASE_FILE_NAME

class FirWasmKlibSerializerFacade(
    testServices: TestServices,
    private val firstTimeCompilation: Boolean
) : IrBackendFacade<BinaryArtifacts.KLib>(testServices, ArtifactKinds.KLib) {

    override val additionalServices: List<ServiceRegistrationData>
        get() = listOf(service(::ModuleDescriptorProvider))

    constructor(testServices: TestServices) : this(testServices, firstTimeCompilation = true)

    override fun shouldTransform(module: TestModule): Boolean {
        return testServices.defaultsProvider.backendKind == inputKind && SKIP_GENERATING_KLIB !in module.directives
    }

    override fun transform(module: TestModule, inputArtifact: IrBackendInput): BinaryArtifacts.KLib {
        require(inputArtifact is IrBackendInput.WasmAfterFrontendBackendInput) {
            "FirWasmKlibSerializerFacade expects IrBackendInput.WasmAfterFrontendBackendInput as input"
        }

        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
        val diagnosticReporter = DiagnosticsCollectorImpl()
        val irDiagnosticReporter = KtDiagnosticReporterWithImplicitIrBasedContext(
            diagnosticReporter,
            configuration.languageVersionSettings
        )
        val outputFile = testServices.klibEnvironmentConfigurator.getKlibArtifactFile(testServices, module.name)

        val target = configuration.get(WasmConfigurationKeys.WASM_TARGET, WasmTarget.JS)

        if (firstTimeCompilation) {
            val moduleName = configuration[CommonConfigurationKeys.MODULE_NAME]!!
            serializeModuleIntoKlib(
                moduleName = moduleName,
                configuration = configuration,
                diagnosticReporter = irDiagnosticReporter,
                metadataSerializer = inputArtifact.metadataSerializer,
                klibPath = outputFile.path,
                moduleFragment = inputArtifact.irModuleFragment,
                irBuiltIns = inputArtifact.irBuiltIns,
                cleanFiles = inputArtifact.icData,
                nopack = true,
                jsOutputName = if (WasmEnvironmentConfigurator.isMainModule(module, testServices)) {
                    WASM_BASE_FILE_NAME
                } else moduleName,
                builtInsPlatform = BuiltInsPlatform.WASM,
                wasmTarget = target,
            )
        }

        val lib = loadWebKlibsInTestPipeline(
            configuration = configuration,
            libraryPaths = listOf(outputFile.path),
            platformChecker = KlibPlatformChecker.Wasm(configuration.wasmTarget.alias)
        ).all.single()

        val moduleDescriptor = JsFactories.DefaultDeserializedDescriptorFactory.createDescriptorOptionalBuiltIns(
            lib,
            configuration.languageVersionSettings,
            LockBasedStorageManager("ModulesStructure"),
            inputArtifact.irModuleFragment.descriptor.builtIns,
            lookupTracker = LookupTracker.DO_NOTHING
        )

        moduleDescriptor.setDependencies(
            inputArtifact.irModuleFragment.descriptor.allDependencyModules.filterIsInstance<ModuleDescriptorImpl>() + moduleDescriptor,
            getFriendDependencies(module, testServices),
        )

        testServices.moduleDescriptorProvider.replaceModuleDescriptorForModule(module, moduleDescriptor)
        testServices.libraryProvider.setDescriptorAndLibraryByName(outputFile.path, moduleDescriptor, lib)

        return BinaryArtifacts.KLib(outputFile, diagnosticReporter)
    }
}
