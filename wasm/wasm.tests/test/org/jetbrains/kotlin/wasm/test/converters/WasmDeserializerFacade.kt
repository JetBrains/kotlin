/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.converters

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContextImpl
import org.jetbrains.kotlin.backend.common.linkage.partial.PartialLinkageConfig
import org.jetbrains.kotlin.backend.common.linkage.partial.PartialLinkageLogLevel
import org.jetbrains.kotlin.backend.common.linkage.partial.PartialLinkageMode
import org.jetbrains.kotlin.backend.common.linkage.partial.setupPartialLinkageConfig
import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureDescriptor
import org.jetbrains.kotlin.backend.wasm.ic.IrFactoryImplForWasmIC
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.ir.backend.js.*
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsManglerDesc
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.library.loader.KlibPlatformChecker
import org.jetbrains.kotlin.psi2ir.generators.TypeTranslatorImpl
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.frontend.classic.moduleDescriptorProvider
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import org.jetbrains.kotlin.test.services.configuration.WasmEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.getDependencies
import org.jetbrains.kotlin.test.services.defaultsProvider
import org.jetbrains.kotlin.test.services.libraryProvider
import org.jetbrains.kotlin.wasm.config.wasmTarget

class WasmDeserializerFacade(
    testServices: TestServices,
) : DeserializerFacade<BinaryArtifacts.KLib, IrBackendInput>(testServices, ArtifactKinds.KLib, BackendKinds.IrBackend) {

    override fun shouldTransform(module: TestModule): Boolean {
        require(testServices.defaultsProvider.backendKind == outputKind)
        return WasmEnvironmentConfigurator.isMainModule(module, testServices)
    }

    override fun transform(module: TestModule, inputArtifact: BinaryArtifacts.KLib): IrBackendInput? {
        require(WasmEnvironmentConfigurator.isMainModule(module, testServices))
        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)

        // TODO: turn on PL only when it's really necessary, see KT-73841
        // Enforce PL with the ERROR log level to fail any tests where PL detected any incompatibilities.
        configuration.setupPartialLinkageConfig(PartialLinkageConfig(PartialLinkageMode.ENABLE, PartialLinkageLogLevel.ERROR))

        val libraries = WasmEnvironmentConfigurator.getAllRecursiveLibrariesFor(module, testServices).map { it.key.libraryFile.canonicalPath }

        val friendLibraries = getDependencies(module, testServices, DependencyRelation.FriendDependency)
            .map(testServices.libraryProvider::getPathByDescriptor)
        val mainModule = MainModule.Klib(inputArtifact.outputFile.absolutePath)
        val project = testServices.compilerConfigurationProvider.getProject(module)

        val klibs = loadWebKlibsInTestPipeline(
            configuration,
            libraryPaths = libraries + mainModule.libPath,
            friendPaths = friendLibraries,
            platformChecker = KlibPlatformChecker.Wasm(configuration.wasmTarget.alias)
        )

        val moduleStructure = ModulesStructure(
            project = project,
            mainModule = mainModule,
            compilerConfiguration = configuration,
            klibs = klibs,
        )

        val moduleInfo = loadIr(
            modulesStructure = moduleStructure,
            irFactory = IrFactoryImplForWasmIC(WholeWorldStageController()),
            loadFunctionInterfacesIntoStdlib = true,
        )

        val symbolTable = SymbolTable(IdSignatureDescriptor(JsManglerDesc), IrFactoryImplForWasmIC(WholeWorldStageController()))
        val moduleDescriptor = testServices.moduleDescriptorProvider.getModuleDescriptor(module)
        val pluginContext = IrPluginContextImpl(
            module = moduleDescriptor,
            bindingContext = BindingContext.EMPTY,
            languageVersionSettings = configuration.languageVersionSettings,
            st = symbolTable,
            typeTranslator = TypeTranslatorImpl(symbolTable, configuration.languageVersionSettings, moduleDescriptor),
            irBuiltIns = moduleInfo.bultins,
            linker = moduleInfo.deserializer,
            messageCollector = configuration.messageCollector,
        )
        return IrBackendInput.WasmDeserializedFromKlibBackendInput(
            moduleInfo,
            irPluginContext = pluginContext,
            klib = inputArtifact.outputFile,
        )
    }
}
