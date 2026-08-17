/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.converters

import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.backend.common.IrBuiltInsForLinker
import org.jetbrains.kotlin.backend.common.IrModuleDependencies
import org.jetbrains.kotlin.backend.common.IrModuleInfo
import org.jetbrains.kotlin.backend.common.LoadedNativeKlibs
import org.jetbrains.kotlin.backend.common.serialization.DeserializationStrategy
import org.jetbrains.kotlin.backend.common.serialization.IrModuleDeserializer
import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureDescriptor
import org.jetbrains.kotlin.backend.konan.serialization.CInteropModuleDeserializerFactory
import org.jetbrains.kotlin.backend.konan.serialization.KonanIrLinker
import org.jetbrains.kotlin.backend.konan.serialization.KonanManglerDesc
import org.jetbrains.kotlin.backend.konan.serialization.loadNativeKlibs
import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.cli.common.diagnosticsCollector
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.PartialLinkageConfig
import org.jetbrains.kotlin.config.PartialLinkageLogLevel
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.ir.InternalSymbolFinderAPI
import org.jetbrains.kotlin.ir.KtDiagnosticReporterWithImplicitIrBasedContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.objcinterop.IrObjCOverridabilityCondition
import org.jetbrains.kotlin.ir.util.ExternalDependenciesGenerator
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.konan.config.konanIncludedLibraries
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.isNativeStdlib
import org.jetbrains.kotlin.library.metadata.KlibMetadataFactories
import org.jetbrains.kotlin.library.metadata.NullFlexibleTypeDeserializer
import org.jetbrains.kotlin.library.metadata.impl.isForwardDeclarationModule
import org.jetbrains.kotlin.library.metadata.kotlinLibrary
import org.jetbrains.kotlin.library.uniqueName
import org.jetbrains.kotlin.native.pipeline.NativeLoadedIrArtifact
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.test.backend.ir.DeserializedFromKlibBackendInput
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.frontend.classic.ModuleDescriptorProvider
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.services.configuration.nativeEnvironmentConfigurator
import java.io.File

class NativeDeserializerFacade(
    testServices: TestServices,
    private val partialLinkageLogLevel: PartialLinkageLogLevel = PartialLinkageLogLevel.ERROR, // Use the ERROR log level by default to fail any tests where PL detected any incompatibilities.
) : DeserializerFacade<BinaryArtifacts.KLib, IrBackendInput>(testServices, ArtifactKinds.KLib, BackendKinds.IrBackend) {

    override val additionalServices: List<ServiceRegistrationData>
        get() = listOf(service(::LibraryProvider), service(::ModuleDescriptorProvider))

    override fun transform(
        module: TestModule,
        inputArtifact: BinaryArtifacts.KLib,
    ): DeserializedFromKlibBackendInput<NativeLoadedIrArtifact> {
        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)

        val includes: List<String> = configuration.konanIncludedLibraries
        val includedPath: String? = includes.singleOrNull()?.let { File(it).canonicalPath }

        val mainLibraryPath: String = inputArtifact.outputFile.canonicalPath
        check(mainLibraryPath == includedPath) {
            "The 'main' library ${inputArtifact.outputFile} should be the single library in the list of included libraries: [${includes.joinToString()}]"
        }

        val loadedKlibs = loadNativeKlibs(configuration, testServices.nativeEnvironmentConfigurator.getNativeTarget(module))
        val [moduleDescriptors, forwardDeclarationsModuleDescriptor] = createModuleDescriptors(configuration, loadedKlibs)
        val moduleInfo = createIrModuleFragments(configuration, loadedKlibs, moduleDescriptors, forwardDeclarationsModuleDescriptor)

        return DeserializedFromKlibBackendInput(NativeLoadedIrArtifact(moduleInfo, configuration), klib = inputArtifact.outputFile)
    }

    private fun createModuleDescriptors(
        configuration: CompilerConfiguration,
        loadedKlibs: LoadedNativeKlibs,
    ): Pair<List<ModuleDescriptorImpl>, ModuleDescriptorImpl> {
        val result = nativeFactories.DefaultResolvedDescriptorsFactory.createResolved2(
            // Note: stdlib goes the first in `LoadedNativeKlibs.all`!
            libraries = loadedKlibs.all,
            storageManager = LockBasedStorageManager.NO_LOCKS,
            builtIns = null,
            languageVersionSettings = configuration.languageVersionSettings,
            friendModuleFiles = loadedKlibs.friends.map { it.path }.toSet(),
            refinesModuleFiles = emptySet(),
            includedLibraryFiles = loadedKlibs.included.map { it.path }.toSet(),
            additionalDependencyModules = emptyList(),
            isForMetadataCompilation = false,
        )
        return result.resolvedDescriptors to result.forwardDeclarationsModule
    }

    private fun createIrModuleFragments(
        configuration: CompilerConfiguration,
        loadedKlibs: LoadedNativeKlibs,
        moduleDescriptors: List<ModuleDescriptorImpl>,
        forwardDeclarationsModuleDescriptor: ModuleDescriptorImpl,
    ): IrModuleInfo {
        val libraryToModuleDescriptor: Map<KotlinLibrary, ModuleDescriptorImpl> = moduleDescriptors.associateBy { it.kotlinLibrary }

        val mainLibrary = loadedKlibs.included.single()
        val mainModuleDescriptor = libraryToModuleDescriptor.getValue(mainLibrary)

        val friendsMap = mapOf(mainLibrary.uniqueName to loadedKlibs.friends.map { it.uniqueName })

        val irDiagnosticReporter = KtDiagnosticReporterWithImplicitIrBasedContext(
            configuration.diagnosticsCollector,
            configuration.languageVersionSettings,
        )

        val symbolTable = SymbolTable(IdSignatureDescriptor(KonanManglerDesc), IrFactoryImpl)

        val irLinker = KonanIrLinker(
            currentModule = mainModuleDescriptor,
            configuration = configuration,
            symbolTable = symbolTable,
            friendModules = friendsMap,
            forwardModuleDescriptor = forwardDeclarationsModuleDescriptor,
            cInteropModuleDeserializerFactory = CInteropModuleDeserializerFactoryMock,
            exportedDependencies = emptyList(),
            partialLinkageConfig = PartialLinkageConfig(partialLinkageLogLevel),
            irDiagnosticReporter = irDiagnosticReporter,
            libraryBeingCached = null,
            externalOverridabilityConditions = listOf(IrObjCOverridabilityCondition),
        )

        val moduleDependencies: IrModuleDependencies = deserializeIrModuleFragments(loadedKlibs.all, irLinker, mainLibrary, libraryToModuleDescriptor::getValue)

        @OptIn(InternalSymbolFinderAPI::class)
        val irBuiltIns = IrBuiltInsForLinker(irLinker, configuration.languageVersionSettings)

        ExternalDependenciesGenerator(symbolTable, listOf(irLinker)).generateUnboundSymbolsAsDependencies()
        irLinker.postProcess(irBuiltIns, inOrAfterLinkageStep = true)

        val sortedModuleDependencies = irLinker.moduleDependencyTracker.reverseTopoOrder(moduleDependencies)

        return IrModuleInfo(
            module = sortedModuleDependencies.included!!,
            dependencies = sortedModuleDependencies,
            bultins = irBuiltIns,
            symbolTable = symbolTable,
            deserializer = irLinker,
        )
    }

    /**
     * Note: [deserializeIrModuleFragments] returns the list of the deserialized [IrModuleFragment]s that has the same
     * order of libraries as in [libraries].
     */
    private inline fun deserializeIrModuleFragments(
        libraries: Collection<KotlinLibrary>,
        irLinker: KonanIrLinker,
        mainModuleLib: KotlinLibrary?,
        mapping: (KotlinLibrary) -> ModuleDescriptor,
    ): IrModuleDependencies {
        val all: MutableList<IrModuleFragment> = mutableListOf()
        var stdlib: IrModuleFragment? = null
        var included: IrModuleFragment? = null

        libraries.forEach { klib: KotlinLibrary ->
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

    companion object {
        @OptIn(K1Deprecation::class)
        private val nativeFactories = KlibMetadataFactories(::KonanBuiltIns, NullFlexibleTypeDeserializer)
    }
}

object CInteropModuleDeserializerFactoryMock : CInteropModuleDeserializerFactory {
    override fun createIrModuleDeserializer(
        moduleFragment: IrModuleFragment,
        klib: KotlinLibrary,
        linker: KonanIrLinker,
    ): IrModuleDeserializer {
        TODO("TODO (KT-85312): Implement IR deserialization for C-interop libraries in tests")
    }
}
