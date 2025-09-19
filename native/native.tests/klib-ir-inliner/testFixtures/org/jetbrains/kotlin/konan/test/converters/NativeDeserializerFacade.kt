/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.converters

import org.jetbrains.kotlin.backend.common.IrModuleDependencies
import org.jetbrains.kotlin.backend.common.IrModuleInfo
import org.jetbrains.kotlin.backend.common.linkage.issues.UserVisibleIrModulesSupport
import org.jetbrains.kotlin.backend.common.linkage.partial.PartialLinkageConfig
import org.jetbrains.kotlin.backend.common.linkage.partial.PartialLinkageLogLevel
import org.jetbrains.kotlin.backend.common.linkage.partial.PartialLinkageMode
import org.jetbrains.kotlin.backend.common.linkage.partial.createPartialLinkageSupportForLinker
import org.jetbrains.kotlin.backend.common.serialization.DescriptorByIdSignatureFinderImpl
import org.jetbrains.kotlin.backend.common.serialization.DeserializationStrategy
import org.jetbrains.kotlin.backend.common.serialization.IrModuleDeserializer
import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureDescriptor
import org.jetbrains.kotlin.backend.common.serialization.sortDependencies
import org.jetbrains.kotlin.backend.konan.KonanStubGeneratorExtensions
import org.jetbrains.kotlin.backend.konan.serialization.CInteropModuleDeserializerFactory
import org.jetbrains.kotlin.backend.konan.serialization.KonanIrLinker
import org.jetbrains.kotlin.backend.konan.serialization.KonanManglerDesc
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.descriptors.IrDescriptorBasedFunctionFactory
import org.jetbrains.kotlin.ir.objcinterop.IrObjCOverridabilityCondition
import org.jetbrains.kotlin.ir.util.ExternalDependenciesGenerator
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.isNativeStdlib
import org.jetbrains.kotlin.library.metadata.impl.isForwardDeclarationModule
import org.jetbrains.kotlin.library.uniqueName
import org.jetbrains.kotlin.psi2ir.descriptors.IrBuiltInsOverDescriptors
import org.jetbrains.kotlin.psi2ir.generators.DeclarationStubGeneratorImpl
import org.jetbrains.kotlin.psi2ir.generators.TypeTranslatorImpl
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput.NativeDeserializedFromKlibBackendInput
import org.jetbrains.kotlin.test.frontend.classic.ModuleDescriptorProvider
import org.jetbrains.kotlin.test.frontend.classic.moduleDescriptorProvider
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.services.configuration.NativeEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.getDependencies

class NativeDeserializerFacade(
    testServices: TestServices,
) : DeserializerFacade<BinaryArtifacts.KLib, IrBackendInput>(testServices, ArtifactKinds.KLib, BackendKinds.IrBackend) {

    override val additionalServices: List<ServiceRegistrationData>
        get() = listOf(service(::LibraryProvider), service(::ModuleDescriptorProvider))

    override fun shouldTransform(module: TestModule): Boolean {
        return testServices.defaultsProvider.backendKind == outputKind
    }

    override fun transform(module: TestModule, inputArtifact: BinaryArtifacts.KLib): NativeDeserializedFromKlibBackendInput? {
        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)

        val moduleInfo = loadIrFromKlib(module, configuration)

        return NativeDeserializedFromKlibBackendInput(moduleInfo, klib = inputArtifact.outputFile)
    }

    private fun loadIrFromKlib(module: TestModule, configuration: CompilerConfiguration): IrModuleInfo {
        val messageCollector = configuration.messageCollector
        val symbolTable = SymbolTable(IdSignatureDescriptor(KonanManglerDesc), IrFactoryImpl)

        val moduleDescriptor = testServices.moduleDescriptorProvider.getModuleDescriptor(module)
        val mainModuleLib = testServices.libraryProvider.getCompiledLibraryByDescriptor(moduleDescriptor)
        val friendLibraries = getDependencies(module, testServices, DependencyRelation.FriendDependency)
            .map { testServices.libraryProvider.getCompiledLibraryByDescriptor(it) }
        val friendModules = mapOf(mainModuleLib.uniqueName to friendLibraries.map { it.uniqueName })

        return getIrModuleInfoForKlib(
            moduleDescriptor,
            sortDependencies(NativeEnvironmentConfigurator.getAllDependenciesMappingFor(module, testServices)) + mainModuleLib,
            friendModules,
            configuration,
            symbolTable,
            messageCollector,
        ) { if (it == mainModuleLib) moduleDescriptor else testServices.libraryProvider.getDescriptorByCompiledLibrary(it) }
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    private inline fun getIrModuleInfoForKlib(
        moduleDescriptor: ModuleDescriptor,
        sortedDependencies: Collection<KotlinLibrary>,
        friendModules: Map<String, List<String>>,
        configuration: CompilerConfiguration,
        symbolTable: SymbolTable,
        messageCollector: MessageCollector,
        mapping: (KotlinLibrary) -> ModuleDescriptor,
    ): IrModuleInfo {
        val mainModuleLib = sortedDependencies.last()
        val typeTranslator = TypeTranslatorImpl(symbolTable, configuration.languageVersionSettings, moduleDescriptor)
        val irBuiltIns = IrBuiltInsOverDescriptors(moduleDescriptor.builtIns, typeTranslator, symbolTable)

        val forwardDeclarationsModuleDescriptor = moduleDescriptor.allDependencyModules.firstOrNull { it.isForwardDeclarationModule }
        val stubGenerator = DeclarationStubGeneratorImpl(
            moduleDescriptor, symbolTable,
            irBuiltIns,
            DescriptorByIdSignatureFinderImpl(moduleDescriptor, KonanManglerDesc),
            KonanStubGeneratorExtensions
        )

        val irLinker = KonanIrLinker(
            currentModule = moduleDescriptor,
            messageCollector = messageCollector,
            builtIns = irBuiltIns,
            symbolTable = symbolTable,
            friendModules = friendModules,
            forwardModuleDescriptor = forwardDeclarationsModuleDescriptor,
            stubGenerator = stubGenerator,
            cInteropModuleDeserializerFactory = CInteropModuleDeserializerFactoryMock,
            exportedDependencies = emptyList(),
            partialLinkageSupport = createPartialLinkageSupportForLinker(
                // TODO KT-77493: Disable PL after all tests for invisible references would be migrated to diagnostic tests
                partialLinkageConfig = PartialLinkageConfig(PartialLinkageMode.ENABLE, PartialLinkageLogLevel.ERROR),
                builtIns = irBuiltIns,
                messageCollector = messageCollector,
            ),
            libraryBeingCached = null,
            userVisibleIrModulesSupport = UserVisibleIrModulesSupport(externalDependenciesLoader = UserVisibleIrModulesSupport.ExternalDependenciesLoader.EMPTY),
            externalOverridabilityConditions = listOf(IrObjCOverridabilityCondition)
        )

        val moduleDependencies: IrModuleDependencies = deserializeDependencies(sortedDependencies, irLinker, mainModuleLib, mapping)

        // TODO: If tests fail due to fictitious synthetic functions, consider passing an instance of
        //  BuiltInFictitiousFunctionIrClassFactory here.
        irBuiltIns.functionFactory = IrDescriptorBasedFunctionFactory(
            irBuiltIns,
            symbolTable,
            typeTranslator,
            getPackageFragment = null,
            true
        )

        irLinker.init(null)
        ExternalDependenciesGenerator(symbolTable, listOf(irLinker)).generateUnboundSymbolsAsDependencies()
        irLinker.postProcess(inOrAfterLinkageStep = true)

        return IrModuleInfo(
            module = moduleDependencies.included!!,
            dependencies = moduleDependencies,
            bultins = irBuiltIns,
            symbolTable = symbolTable,
            deserializer = irLinker,
        )
    }

    /**
     * Note: [deserializeDependencies] returns the list of the deserialized [IrModuleFragment]s that has the same
     * order of libraries as in [sortedDependencies].
     */
    private inline fun deserializeDependencies(
        sortedDependencies: Collection<KotlinLibrary>,
        irLinker: KonanIrLinker,
        mainModuleLib: KotlinLibrary?,
        mapping: (KotlinLibrary) -> ModuleDescriptor,
    ): IrModuleDependencies {
        val all: MutableList<IrModuleFragment> = mutableListOf()
        var stdlib: IrModuleFragment? = null
        var included: IrModuleFragment? = null

        sortedDependencies.forEach { klib: KotlinLibrary ->
            val descriptor: ModuleDescriptor = mapping(klib)
            val module: IrModuleFragment = if (klib != mainModuleLib)
                irLinker.deserializeIrModuleHeader(descriptor, klib, { DeserializationStrategy.EXPLICITLY_EXPORTED })
            else
                irLinker.deserializeIrModuleHeader(descriptor, klib, { DeserializationStrategy.ALL }, descriptor.name.asString())

            all += module
            when {
                klib.isNativeStdlib -> stdlib = module
                klib == mainModuleLib -> included = module
            }
        }

        return IrModuleDependencies(
            all = all,
            stdlib = stdlib,
            included = included,
        )
    }
}

object CInteropModuleDeserializerFactoryMock : CInteropModuleDeserializerFactory {
    override fun createIrModuleDeserializer(
        moduleDescriptor: ModuleDescriptor,
        klib: KotlinLibrary,
        moduleDependencies: Collection<IrModuleDeserializer>,
    ): IrModuleDeserializer {
        TODO("Not yet implemented")
    }
}
