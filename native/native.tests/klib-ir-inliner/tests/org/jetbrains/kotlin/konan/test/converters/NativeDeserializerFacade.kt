/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.converters


import org.jetbrains.kotlin.backend.common.IrModuleInfo
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContextImpl
import org.jetbrains.kotlin.backend.common.linkage.partial.createPartialLinkageSupportForLinker
import org.jetbrains.kotlin.backend.common.serialization.DeserializationStrategy
import org.jetbrains.kotlin.backend.common.serialization.KotlinIrLinker
import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureDescriptor
import org.jetbrains.kotlin.backend.konan.serialization.KonanManglerDesc
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.backend.js.*
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsIrLinker
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.descriptors.IrDescriptorBasedFunctionFactory
import org.jetbrains.kotlin.ir.linkage.partial.*
import org.jetbrains.kotlin.ir.util.ExternalDependenciesGenerator
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.uniqueName
import org.jetbrains.kotlin.psi2ir.descriptors.IrBuiltInsOverDescriptors
import org.jetbrains.kotlin.psi2ir.generators.TypeTranslatorImpl
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput.NativeDeserializedFromKlibBackendInput
import org.jetbrains.kotlin.test.frontend.classic.moduleDescriptorProvider
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import org.jetbrains.kotlin.test.services.configuration.NativeEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.getDependencies
import org.jetbrains.kotlin.test.services.libraryProvider
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue
import java.io.File
import kotlin.collections.component1
import kotlin.collections.component2

class NativeDeserializerFacade(
    testServices: TestServices,
) : DeserializerFacade<BinaryArtifacts.KLib, IrBackendInput>(testServices, ArtifactKinds.KLib, BackendKinds.IrBackend) {

    override fun shouldRunAnalysis(module: TestModule): Boolean {
        return module.backendKind == outputKind
    }

    override fun transform(module: TestModule, inputArtifact: BinaryArtifacts.KLib): NativeDeserializedFromKlibBackendInput? {
        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)

        // Enforce PL with the ERROR log level to fail any tests where PL detected any incompatibilities.
        configuration.setupPartialLinkageConfig(PartialLinkageConfig(PartialLinkageMode.ENABLE, PartialLinkageLogLevel.ERROR))

        val friendLibraries = getDependencies(module, testServices, DependencyRelation.FriendDependency)
            .map(testServices.libraryProvider::getPathByDescriptor)
        val mainModule = MainModule.Klib(inputArtifact.outputFile.absolutePath)
        val project = testServices.compilerConfigurationProvider.getProject(module)
        val moduleStructure = ModulesStructure(
            project,
            mainModule,
            configuration,
            NativeEnvironmentConfigurator.getRuntimePathsForModule(module, testServices) + mainModule.libPath,
            friendLibraries
        )

        val moduleInfo = loadNativeIr(
            depsDescriptors = moduleStructure,
            irFactory = IrFactoryImpl,
            loadFunctionInterfacesIntoStdlib = true,
        )

        val symbolTable = SymbolTable(IdSignatureDescriptor(KonanManglerDesc), IrFactoryImpl)
        val moduleDescriptor = testServices.moduleDescriptorProvider.getModuleDescriptor(module)
        val pluginContext = IrPluginContextImpl(
            module = moduleDescriptor,
            bindingContext = BindingContext.EMPTY,
            languageVersionSettings = configuration.languageVersionSettings,
            st = symbolTable,
            typeTranslator = TypeTranslatorImpl(symbolTable, configuration.languageVersionSettings, moduleDescriptor),
            irBuiltIns = moduleInfo.bultins,
            linker = moduleInfo.deserializer,
            diagnosticReporter = configuration.messageCollector,
        )
        return NativeDeserializedFromKlibBackendInput(
            moduleInfo,
            irPluginContext = pluginContext,
            diagnosticReporter = DiagnosticReporterFactory.createReporter(configuration.messageCollector),
            klib = inputArtifact.outputFile,
        )
    }
}

fun loadNativeIr(
    depsDescriptors: ModulesStructure,
    irFactory: IrFactory,
    filesToLoad: Set<String>? = null,
    loadFunctionInterfacesIntoStdlib: Boolean = false,
): IrModuleInfo {
    val mainModule = depsDescriptors.mainModule
    val configuration = depsDescriptors.compilerConfiguration
    val allDependencies = depsDescriptors.allDependencies
    val messageLogger = configuration.messageCollector

    val signaturer = IdSignatureDescriptor(KonanManglerDesc)
    val symbolTable = SymbolTable(signaturer, irFactory)

    when (mainModule) {
        is MainModule.SourceFiles -> {
            error("MainModule.SourceFiles is not supported by NativeDeserializerFacade")
        }
        is MainModule.Klib -> {
            val mainPath = File(mainModule.libPath).canonicalPath
            val mainModuleLib = allDependencies.find { it.libraryFile.canonicalPath == mainPath }
                ?: error("No module with ${mainModule.libPath} found")
            val moduleDescriptor = depsDescriptors.getModuleDescriptor(mainModuleLib)
            val sortedDependencies = sortDependencies(depsDescriptors.moduleDependencies)
            val friendModules = mapOf(mainModuleLib.uniqueName to depsDescriptors.friendDependencies.map { it.uniqueName })

            return getIrModuleInfoForKlib(
                moduleDescriptor,
                sortedDependencies,
                friendModules,
                filesToLoad,
                configuration,
                symbolTable,
                messageLogger,
                loadFunctionInterfacesIntoStdlib,
            ) { depsDescriptors.getModuleDescriptor(it) }
        }
    }
}

@OptIn(ObsoleteDescriptorBasedAPI::class)
fun getIrModuleInfoForKlib(
    moduleDescriptor: ModuleDescriptor,
    sortedDependencies: Collection<KotlinLibrary>,
    friendModules: Map<String, List<String>>,
    filesToLoad: Set<String>?,
    configuration: CompilerConfiguration,
    symbolTable: SymbolTable,
    messageCollector: MessageCollector,
    loadFunctionInterfacesIntoStdlib: Boolean,
    mapping: (KotlinLibrary) -> ModuleDescriptor,
): IrModuleInfo {
    val mainModuleLib = sortedDependencies.last()
    val typeTranslator = TypeTranslatorImpl(symbolTable, configuration.languageVersionSettings, moduleDescriptor)
    val irBuiltIns = IrBuiltInsOverDescriptors(moduleDescriptor.builtIns, typeTranslator, symbolTable)

    // TODO Move KonanrIrLinker out of backend.native and use it here instead of JsIrLinker
    val irLinker: KotlinIrLinker = JsIrLinker(
        currentModule = null,
        messageCollector = messageCollector,
        builtIns = irBuiltIns,
        symbolTable = symbolTable,
        partialLinkageSupport = createPartialLinkageSupportForLinker(
            partialLinkageConfig = configuration.partialLinkageConfig,
            builtIns = irBuiltIns,
            messageCollector = messageCollector
        ),
        translationPluginContext = null,
        icData = null,
        friendModules = friendModules
    )

    val deserializedModuleFragmentsToLib = deserializeDependencies(sortedDependencies, irLinker, mainModuleLib, filesToLoad, mapping)
    val deserializedModuleFragments = deserializedModuleFragmentsToLib.keys.toList()
    irBuiltIns.functionFactory = IrDescriptorBasedFunctionFactory(
        irBuiltIns,
        symbolTable,
        typeTranslator,
        loadFunctionInterfacesIntoStdlib.ifTrue {
            FunctionTypeInterfacePackages().makePackageAccessor(deserializedModuleFragments.first())
        },
        true
    )

    val moduleFragment = deserializedModuleFragments.last()

    irLinker.init(null, emptyList())
    ExternalDependenciesGenerator(symbolTable, listOf(irLinker)).generateUnboundSymbolsAsDependencies()
    irLinker.postProcess(inOrAfterLinkageStep = true)

    return IrModuleInfo(
        moduleFragment,
        deserializedModuleFragments,
        irBuiltIns,
        symbolTable,
        irLinker,
        deserializedModuleFragmentsToLib.getUniqueNameForEachFragment()
    )
}

fun deserializeDependencies(
    sortedDependencies: Collection<KotlinLibrary>,
    irLinker: KotlinIrLinker,
    mainModuleLib: KotlinLibrary?,
    filesToLoad: Set<String>?,
    mapping: (KotlinLibrary) -> ModuleDescriptor
): Map<IrModuleFragment, KotlinLibrary> {
    return sortedDependencies.associateBy { klib ->
        val descriptor = mapping(klib)
        when {
            mainModuleLib == null -> irLinker.deserializeIrModuleHeader(descriptor, klib, { DeserializationStrategy.EXPLICITLY_EXPORTED })
            filesToLoad != null && klib == mainModuleLib -> irLinker.deserializeDirtyFiles(descriptor, klib, filesToLoad)
            filesToLoad != null && klib != mainModuleLib -> irLinker.deserializeHeadersWithInlineBodies(descriptor, klib)
            klib == mainModuleLib -> irLinker.deserializeIrModuleHeader(descriptor, klib, { DeserializationStrategy.ALL })
            else -> irLinker.deserializeIrModuleHeader(descriptor, klib, { DeserializationStrategy.EXPLICITLY_EXPORTED })
        }
    }
}

private fun Map<IrModuleFragment, KotlinLibrary>.getUniqueNameForEachFragment(): Map<IrModuleFragment, String> {
    return this.entries.mapNotNull { (moduleFragment, klib) ->
        klib.jsOutputName?.let { moduleFragment to it }
    }.toMap()
}
