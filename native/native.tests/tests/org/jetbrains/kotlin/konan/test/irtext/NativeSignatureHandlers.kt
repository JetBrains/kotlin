/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.irtext

import org.jetbrains.kotlin.backend.common.overrides.IrLinkerFakeOverrideProvider
import org.jetbrains.kotlin.backend.common.serialization.BasicIrModuleDeserializer
import org.jetbrains.kotlin.backend.common.serialization.DeserializationStrategy
import org.jetbrains.kotlin.backend.common.serialization.IrModuleDeserializer
import org.jetbrains.kotlin.backend.common.serialization.KotlinIrLinker
import org.jetbrains.kotlin.backend.konan.serialization.KonanFakeOverrideClassFilter
import org.jetbrains.kotlin.backend.konan.serialization.KonanManglerDesc
import org.jetbrains.kotlin.backend.konan.serialization.KonanManglerIr
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.descriptors.konan.isNativeStdlib
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.types.IrTypeSystemContextImpl
import org.jetbrains.kotlin.ir.util.IrMessageLogger
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.konan.test.Fir2IrNativeResultsConverter
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.impl.createKotlinLibraryComponents
import org.jetbrains.kotlin.library.isNativeStdlib
import org.jetbrains.kotlin.library.metadata.KlibMetadataFactories
import org.jetbrains.kotlin.library.metadata.KlibMetadataModuleDescriptorFactory
import org.jetbrains.kotlin.library.metadata.NullFlexibleTypeDeserializer
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.test.backend.handlers.*
import org.jetbrains.kotlin.test.backend.handlers.AbstractDescriptorAwareVerifyIdSignaturesByKlib.LoadedModules
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.configuration.NativeEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import java.nio.file.Path
import java.nio.file.Paths
import org.jetbrains.kotlin.konan.file.File as KFile

class NativeCollectAndMemorizeIdSignatures(
    testServices: TestServices
) : AbstractCollectAndMemorizeIdSignatures(testServices, KonanManglerIr)

class NativeVerifyIdSignaturesByDeserializedIr(
    testServices: TestServices
) : AbstractVerifyIdSignaturesByDeserializedIr(testServices, KonanManglerDesc) {
    override fun loadModules(libraries: Collection<KotlinLibrary>) = loadNativeModules(testServices, libraries)
    override val createIrLinker: (IrMessageLogger, IrBuiltIns, SymbolTable) -> KotlinIrLinker get() = ::LightWeightKonanLinker
}

class NativeVerifyIdSignaturesByK1LazyIr(
    testServices: TestServices
) : AbstractVerifyIdSignaturesByK1LazyIr(testServices, KonanManglerDesc) {
    override fun loadModules(libraries: Collection<KotlinLibrary>) = loadNativeModules(testServices, libraries)
    override val createIrLinker: (IrMessageLogger, IrBuiltIns, SymbolTable) -> KotlinIrLinker get() = ::LightWeightKonanLinker
}

class NativeVerifyIdSignaturesByK2LazyIr(
    testServices: TestServices
) : AbstractVerifyIdSignaturesByK2LazyIr(testServices) {
    override val fir2IrConverter get() = ::Fir2IrNativeResultsConverter
}

private fun loadNativeModules(testServices: TestServices, libraries: Collection<KotlinLibrary>): LoadedModules =
    loadModules(
        testServices,
        libraries,
        runtimeLibraryPaths = {
            val arbitraryModule = testServices.moduleStructure.modules.first()
            NativeEnvironmentConfigurator.getRuntimePathsForModule(arbitraryModule, testServices).map(Paths::get)
        },
        isStdlib = KotlinLibrary::isNativeStdlib,
        factories = { KlibMetadataFactories(::KonanBuiltIns, NullFlexibleTypeDeserializer) }
    )

private inline fun loadModules(
    testServices: TestServices,
    libraries: Collection<KotlinLibrary>,
    runtimeLibraryPaths: () -> Collection<Path>,
    isStdlib: (KotlinLibrary) -> Boolean,
    factories: () -> KlibMetadataFactories
): LoadedModules {
    val runtimeLibraries = runtimeLibraryPaths().map { libraryPath ->
        val libraryComponents = createKotlinLibraryComponents(KFile(libraryPath))
        libraryComponents.singleOrNull()
            ?: testServices.assertions.fail { "Only 1 component expected in library '$libraryPath', found: ${libraryComponents.size}" }
    }

    val stdlib = runtimeLibraries.firstOrNull(isStdlib)
        ?: testServices.assertions.fail { "Stdlib not found across runtime libraries: ${runtimeLibraries.joinToString { it.libraryFile.path }}" }

    with(factories().DefaultDeserializedDescriptorFactory) {
        val stdlibModule = createStdlibModule(stdlib)
        stdlibModule.setDependencies(stdlibModule)

        val builtIns = stdlibModule.builtIns

        val otherRuntimeModules = runtimeLibraries.mapNotNull { library ->
            runIf(library != stdlib) { createModule(library, builtIns) }
        }
        val runtimeModules = listOf(stdlibModule) + otherRuntimeModules
        otherRuntimeModules.forEach { module -> module.setDependencies(runtimeModules) }

        val libraryModules = libraries.map { library -> createModule(library, builtIns) }
        val allModules = runtimeModules + libraryModules
        libraryModules.forEach { module -> module.setDependencies(allModules) }

        return LoadedModules(libraryModules, runtimeModules)
    }
}

private fun KlibMetadataModuleDescriptorFactory.createStdlibModule(stdlib: KotlinLibrary): ModuleDescriptorImpl =
    createDescriptorAndNewBuiltIns(
        stdlib,
        LanguageVersionSettingsImpl.DEFAULT,
        LockBasedStorageManager.NO_LOCKS,
        packageAccessHandler = null
    )

private fun KlibMetadataModuleDescriptorFactory.createModule(library: KotlinLibrary, builtIns: KotlinBuiltIns): ModuleDescriptorImpl =
    createDescriptorOptionalBuiltIns(
        library,
        LanguageVersionSettingsImpl.DEFAULT,
        LockBasedStorageManager.NO_LOCKS,
        builtIns,
        packageAccessHandler = null,
        LookupTracker.DO_NOTHING
    )

// Note: Only for tests.
// Caution: No support for C-interop and static caches yet.
private class LightWeightKonanLinker(
    irMessageLogger: IrMessageLogger,
    irBuiltIns: IrBuiltIns,
    symbolTable: SymbolTable
) : KotlinIrLinker(
    currentModule = null,
    irMessageLogger,
    irBuiltIns,
    symbolTable,
    exportedDependencies = emptyList()
) {
    override val fakeOverrideBuilder = IrLinkerFakeOverrideProvider(
        linker = this,
        symbolTable = symbolTable,
        mangler = KonanManglerIr,
        typeSystem = IrTypeSystemContextImpl(irBuiltIns),
        friendModules = emptyMap(),
        partialLinkageSupport = partialLinkageSupport,
        platformSpecificClassFilter = KonanFakeOverrideClassFilter
    )

    override val translationPluginContext get() = null

    override fun createModuleDeserializer(
        moduleDescriptor: ModuleDescriptor,
        klib: KotlinLibrary?,
        strategyResolver: (String) -> DeserializationStrategy,
    ): IrModuleDeserializer {
        require(klib != null) { "Expecting kotlin library for $moduleDescriptor" }

        return object : BasicIrModuleDeserializer(
            linker = this,
            moduleDescriptor = moduleDescriptor,
            klib = klib,
            strategyResolver = strategyResolver,
            libraryAbiVersion = klib.versions.abiVersion ?: KotlinAbiVersion.CURRENT
        ) {}
    }

    override fun isBuiltInModule(moduleDescriptor: ModuleDescriptor): Boolean = moduleDescriptor.isNativeStdlib()
}
