/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.converters

import org.jetbrains.kotlin.backend.common.linkage.partial.PartialLinkageConfig
import org.jetbrains.kotlin.backend.common.linkage.partial.PartialLinkageLogLevel
import org.jetbrains.kotlin.backend.common.linkage.partial.PartialLinkageMode
import org.jetbrains.kotlin.backend.common.linkage.partial.setupPartialLinkageConfig
import org.jetbrains.kotlin.ir.backend.js.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImplForJsIC
import org.jetbrains.kotlin.js.test.utils.JsIrIncrementalDataProvider
import org.jetbrains.kotlin.library.loader.KlibPlatformChecker
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

        // Enforce PL with the ERROR log level to fail any tests where PL detected any incompatibilities.
        configuration.setupPartialLinkageConfig(PartialLinkageConfig(PartialLinkageMode.ENABLE, PartialLinkageLogLevel.ERROR))

        val runtimeKlibs = JsEnvironmentConfigurator.getRuntimePathsForModule(module, testServices)
        val klibDependencies = getKlibDependencies(module, testServices, DependencyRelation.RegularDependency)
            .map { it.absolutePath }
        val klibFriendDependencies = getKlibDependencies(module, testServices, DependencyRelation.FriendDependency)
            .map { it.absolutePath }
        val mainModule = MainModule.Klib(inputArtifact.outputFile.absolutePath)
        val mainPath = File(mainModule.libPath).canonicalPath

        val klibs = loadWebKlibsInTestPipeline(
            configuration = configuration,
            libraryPaths = runtimeKlibs + klibDependencies + klibFriendDependencies + mainPath,
            friendPaths = klibFriendDependencies,
            includedPath = mainPath,
            platformChecker = KlibPlatformChecker.JS,
        )

        val modulesStructure = ModulesStructure(
            project = testServices.compilerConfigurationProvider.getProject(module),
            mainModule = mainModule,
            compilerConfiguration = configuration,
            klibs = klibs,
        )

        val filesToLoad = module.files.takeIf { !firstTimeCompilation }?.map { "/${it.relativePath}" }?.toSet()
        val mainModuleLib = klibs.included ?: error("No module with ${mainModule.libPath} found")

        val moduleInfo = loadIr(
            modulesStructure,
            IrFactoryImplForJsIC(WholeWorldStageController()),
            filesToLoad,
            loadFunctionInterfacesIntoStdlib = true,
        )

        // Some test downstream handlers like JsSourceMapPathRewriter expect a module descriptor to be present.
        testServices.moduleDescriptorProvider.replaceModuleDescriptorForModule(module, modulesStructure.getModuleDescriptor(mainModuleLib))
        for (library in klibs.all) {
            testServices.libraryProvider.setDescriptorAndLibraryByName(
                library.libraryFile.canonicalPath,
                modulesStructure.getModuleDescriptor(library),
                library
            )
        }

        return IrBackendInput.JsIrDeserializedFromKlibBackendInput(moduleInfo, klib = inputArtifact.outputFile)
    }
}
