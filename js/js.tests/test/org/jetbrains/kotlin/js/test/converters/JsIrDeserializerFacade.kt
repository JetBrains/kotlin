/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.converters

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContextImpl
import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureDescriptor
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.ir.backend.js.MainModule
import org.jetbrains.kotlin.ir.backend.js.ModulesStructure
import org.jetbrains.kotlin.ir.backend.js.WholeWorldStageController
import org.jetbrains.kotlin.ir.backend.js.loadIr
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsManglerDesc
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImplForJsIC
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.js.test.utils.JsIrIncrementalDataProvider
import org.jetbrains.kotlin.psi2ir.generators.TypeTranslatorImpl
import org.jetbrains.kotlin.resolve.BindingContext.EMPTY
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.frontend.classic.ModuleDescriptorProvider
import org.jetbrains.kotlin.test.frontend.classic.moduleDescriptorProvider
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.getKlibDependencies
import java.io.File

open class JsIrDeserializerFacade(
    testServices: TestServices,
    private val firstTimeCompilation: Boolean = true,
) : DeserializerFacade<BinaryArtifacts.KLib, IrBackendInput>(testServices, ArtifactKinds.KLib, BackendKinds.IrBackend) {
    override val additionalServices: List<ServiceRegistrationData>
        get() = listOf(
            service(::ModuleDescriptorProvider),
            service(::JsIrIncrementalDataProvider),
            service(::LibraryProvider)
        )

    override fun shouldTransform(module: TestModule): Boolean {
        return testServices.defaultsProvider.backendKind == outputKind
    }

    override fun transform(module: TestModule, inputArtifact: BinaryArtifacts.KLib): IrBackendInput? {
        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)

        val runtimeKlibs = JsEnvironmentConfigurator.getRuntimePathsForModule(module, testServices)
        val klibDependencies = getKlibDependencies(module, testServices, DependencyRelation.RegularDependency)
            .map { it.absolutePath }
        val klibFriendDependencies = getKlibDependencies(module, testServices, DependencyRelation.FriendDependency)
            .map { it.absolutePath }
        val mainModule = MainModule.Klib(inputArtifact.outputFile.absolutePath)
        val mainPath = File(mainModule.libPath).canonicalPath
        val modulesStructure = ModulesStructure(
            project = testServices.compilerConfigurationProvider.getProject(module),
            mainModule = mainModule,
            compilerConfiguration = configuration,
            libraryPaths = runtimeKlibs + klibDependencies + klibFriendDependencies + mainPath,
            friendDependenciesPaths = klibFriendDependencies,
        )

        val filesToLoad = module.files.takeIf { !firstTimeCompilation }?.map { "/${it.relativePath}" }?.toSet()
        val messageCollector = configuration.messageCollector
        val symbolTable = SymbolTable(IdSignatureDescriptor(JsManglerDesc), IrFactoryImplForJsIC(WholeWorldStageController()))
        val mainModuleLib = modulesStructure.allDependencies.find { it.libraryFile.canonicalPath == mainPath }
            ?: error("No module with ${mainModule.libPath} found")

        val moduleInfo = loadIr(
            modulesStructure,
            IrFactoryImplForJsIC(WholeWorldStageController()),
            filesToLoad,
            loadFunctionInterfacesIntoStdlib = true,
        )

        // This is only needed to create the plugin context, which may be required by the downstream test handlers.
        // Most of the time those handlers use it only for obtaining IrBuiltIns.
        // It would be good to fix this.
        val moduleDescriptor = modulesStructure.getModuleDescriptor(mainModuleLib)

        // Some test downstream handlers like JsSourceMapPathRewriter expect a module descriptor to be present.
        testServices.moduleDescriptorProvider.replaceModuleDescriptorForModule(module, moduleDescriptor)
        for (library in modulesStructure.allDependencies) {
            testServices.libraryProvider.setDescriptorAndLibraryByName(
                library.libraryFile.canonicalPath,
                modulesStructure.getModuleDescriptor(library),
                library
            )
        }

        val pluginContext = IrPluginContextImpl(
            module = modulesStructure.getModuleDescriptor(mainModuleLib),
            bindingContext = EMPTY,
            languageVersionSettings = configuration.languageVersionSettings,
            st = symbolTable,
            typeTranslator = TypeTranslatorImpl(symbolTable, configuration.languageVersionSettings, moduleDescriptor),
            irBuiltIns = moduleInfo.bultins,
            linker = moduleInfo.deserializer,
            messageCollector = messageCollector,
        )

        return IrBackendInput.JsIrDeserializedFromKlibBackendInput(
            moduleInfo,
            irPluginContext = pluginContext,
            klib = inputArtifact.outputFile,
        )
    }
}
