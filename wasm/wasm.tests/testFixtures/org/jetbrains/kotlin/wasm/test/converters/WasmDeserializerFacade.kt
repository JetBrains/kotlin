/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.converters

import org.jetbrains.kotlin.backend.common.IrModuleInfo
import org.jetbrains.kotlin.backend.common.LoadedKlibs
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContextImpl
import org.jetbrains.kotlin.backend.common.linkage.partial.PartialLinkageConfig
import org.jetbrains.kotlin.backend.common.linkage.partial.PartialLinkageLogLevel
import org.jetbrains.kotlin.backend.common.linkage.partial.PartialLinkageMode
import org.jetbrains.kotlin.backend.common.linkage.partial.setupPartialLinkageConfig
import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureDescriptor
import org.jetbrains.kotlin.backend.wasm.ic.IrFactoryImplForWasmIC
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.ir.backend.js.MainModule
import org.jetbrains.kotlin.ir.backend.js.ModulesStructure
import org.jetbrains.kotlin.ir.backend.js.WholeWorldStageController
import org.jetbrains.kotlin.ir.backend.js.loadIr
import org.jetbrains.kotlin.ir.backend.js.loadIrForSingleModule
import org.jetbrains.kotlin.ir.backend.js.loadWebKlibsInTestPipeline
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsManglerDesc
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.isWasmKotlinTest
import org.jetbrains.kotlin.library.isWasmStdlib
import org.jetbrains.kotlin.library.loader.KlibPlatformChecker
import org.jetbrains.kotlin.psi2ir.generators.TypeTranslatorImpl
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.frontend.classic.ModuleDescriptorProvider
import org.jetbrains.kotlin.test.frontend.classic.moduleDescriptorProvider
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.LibraryProvider
import org.jetbrains.kotlin.test.services.ServiceRegistrationData
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import org.jetbrains.kotlin.test.services.configuration.WasmEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.getKlibDependencies
import org.jetbrains.kotlin.test.services.defaultsProvider
import org.jetbrains.kotlin.test.services.libraryProvider
import org.jetbrains.kotlin.test.services.service
import org.jetbrains.kotlin.wasm.config.wasmTarget
import java.io.File

enum class SingleModuleType {
    STDLIB, KOTLIN_TEST, TEST_MODULE
}

class WasmDeserializerSingleModuleFacade(testServices: TestServices, private val singleModuleType: SingleModuleType) :
    WasmDeserializerFacadeBase(testServices) {

    override fun getMainModule(module: TestModule, inputArtifact: BinaryArtifacts.KLib): MainModule.Klib {
        val libraries = WasmEnvironmentConfigurator.getDependencyLibrariesFor(module, testServices)
        return when (singleModuleType) {
            SingleModuleType.STDLIB -> MainModule.Klib(libraries.single { it.isWasmStdlib }.libraryFile.absolutePath)
            SingleModuleType.KOTLIN_TEST -> MainModule.Klib(libraries.single { it.isWasmKotlinTest }.libraryFile.absolutePath)
            SingleModuleType.TEST_MODULE -> super.getMainModule(module, inputArtifact)
        }
    }

    override fun loadKLibs(module: TestModule, mainModule: MainModule.Klib, configuration: CompilerConfiguration): LoadedKlibs {
        val libraries = WasmEnvironmentConfigurator.getDependencyLibrariesFor(module, testServices)
        return when (singleModuleType) {
            SingleModuleType.STDLIB -> {
                val stdlib = libraries.single { it.isWasmStdlib }
                LoadedKlibs(
                    all = listOf(stdlib),
                    friends = emptyList(),
                    included = stdlib
                )
            }
            SingleModuleType.KOTLIN_TEST -> {
                check(libraries.size == 2) //stdlib and kotlin.test
                LoadedKlibs(
                    all = libraries,
                    friends = emptyList(),
                    included = libraries.single { it.isWasmKotlinTest }
                )
            }
            SingleModuleType.TEST_MODULE -> {
                super.loadKLibs(module, mainModule, configuration)
            }
        }
    }

    override fun loadKLibsIr(modulesStructure: ModulesStructure): IrModuleInfo =
        loadIrForSingleModule(modulesStructure, IrFactoryImplForWasmIC(WholeWorldStageController()))

    override fun shouldTransform(module: TestModule): Boolean = true
}

class WasmDeserializerFacade(testServices: TestServices) : WasmDeserializerFacadeBase(testServices) {
    override fun loadKLibsIr(modulesStructure: ModulesStructure): IrModuleInfo {
        return loadIr(
            modulesStructure = modulesStructure,
            irFactory = IrFactoryImplForWasmIC(WholeWorldStageController()),
            loadFunctionInterfacesIntoStdlib = true,
        )
    }

    override fun shouldTransform(module: TestModule): Boolean {
        require(testServices.defaultsProvider.backendKind == outputKind)
        return WasmEnvironmentConfigurator.isMainModule(module, testServices)
    }
}

abstract class WasmDeserializerFacadeBase(
    testServices: TestServices,
) : DeserializerFacade<BinaryArtifacts.KLib, IrBackendInput>(testServices, ArtifactKinds.KLib, BackendKinds.IrBackend) {

    abstract fun loadKLibsIr(modulesStructure: ModulesStructure): IrModuleInfo

    open fun getMainModule(module: TestModule, inputArtifact: BinaryArtifacts.KLib): MainModule.Klib =
        MainModule.Klib(inputArtifact.outputFile.absolutePath)

    open fun loadKLibs(module: TestModule, mainModule: MainModule.Klib, configuration: CompilerConfiguration): LoadedKlibs {
        val wasmTarget = configuration.wasmTarget

        val runtimeKlibs: List<String> = WasmEnvironmentConfigurator.getRuntimePathsForModule(wasmTarget)
        val klibDependencies: List<String> = getKlibDependencies(module, testServices, DependencyRelation.RegularDependency)
            .map { it.absolutePath }
        val klibFriendDependencies: List<String> = getKlibDependencies(module, testServices, DependencyRelation.FriendDependency)
            .map { it.absolutePath }

        val mainPath = File(mainModule.libPath).canonicalPath

        return loadWebKlibsInTestPipeline(
            configuration = configuration,
            libraryPaths = runtimeKlibs + klibDependencies + klibFriendDependencies + mainPath,
            friendPaths = klibFriendDependencies,
            includedPath = mainPath,
            platformChecker = KlibPlatformChecker.Wasm(wasmTarget.alias)
        )
    }

    override val additionalServices: List<ServiceRegistrationData>
        get() = listOf(
            service(::ModuleDescriptorProvider),
            service(::LibraryProvider)
        )

    override fun transform(module: TestModule, inputArtifact: BinaryArtifacts.KLib): IrBackendInput? {
        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)

        // Enforce PL with the ERROR log level to fail any tests where PL detected any incompatibilities.
        configuration.setupPartialLinkageConfig(PartialLinkageConfig(PartialLinkageMode.ENABLE, PartialLinkageLogLevel.ERROR))

        val mainModule = getMainModule(module, inputArtifact)

        val klibs = loadKLibs(module, mainModule, configuration)

        val modulesStructure = ModulesStructure(
            project = testServices.compilerConfigurationProvider.getProject(module),
            mainModule = mainModule,
            compilerConfiguration = configuration,
            klibs = klibs,
        )

        val symbolTable = SymbolTable(IdSignatureDescriptor(JsManglerDesc), IrFactoryImplForWasmIC(WholeWorldStageController()))
        val mainModuleLib: KotlinLibrary = klibs.included ?: error("No module with ${mainModule.libPath} found")

        val moduleInfo = loadKLibsIr(modulesStructure)

        // This is only needed to create the plugin context, which may be required by the downstream test handlers.
        // Most of the time those handlers use it only for obtaining IrBuiltIns.
        // It would be good to fix this.
        val mainModuleDescriptor = modulesStructure.getModuleDescriptor(mainModuleLib)

        // Some test downstream handlers like JsSourceMapPathRewriter expect a module descriptor to be present.
        testServices.moduleDescriptorProvider.replaceModuleDescriptorForModule(module, mainModuleDescriptor)
        for (library in klibs.all) {
            testServices.libraryProvider.setDescriptorAndLibraryByName(
                library.libraryFile.canonicalPath,
                modulesStructure.getModuleDescriptor(library),
                library
            )
        }

        val pluginContext = IrPluginContextImpl(
            module = mainModuleDescriptor,
            bindingContext = BindingContext.EMPTY,
            languageVersionSettings = configuration.languageVersionSettings,
            st = symbolTable,
            typeTranslator = TypeTranslatorImpl(symbolTable, configuration.languageVersionSettings, mainModuleDescriptor),
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