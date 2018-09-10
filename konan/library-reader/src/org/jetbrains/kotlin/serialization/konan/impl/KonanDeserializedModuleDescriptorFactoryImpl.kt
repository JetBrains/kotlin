/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization.konan.impl

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.descriptors.konan.DeserializedKonanModuleOrigin
import org.jetbrains.kotlin.descriptors.konan.KonanModuleDescriptorFactory
import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.konan.library.resolver.PackageAccessedHandler
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.CompilerDeserializationConfiguration
import org.jetbrains.kotlin.serialization.konan.KonanDeserializedModuleDescriptorFactory
import org.jetbrains.kotlin.serialization.konan.KonanDeserializedPackageFragmentsFactory
import org.jetbrains.kotlin.storage.StorageManager

internal class KonanDeserializedModuleDescriptorFactoryImpl(
    override val descriptorFactory: KonanModuleDescriptorFactory,
    override val packageFragmentsFactory: KonanDeserializedPackageFragmentsFactory
) : KonanDeserializedModuleDescriptorFactory {

    override fun createDescriptor(
        library: KonanLibrary,
        languageVersionSettings: LanguageVersionSettings,
        storageManager: StorageManager,
        builtIns: KotlinBuiltIns,
        packageAccessedHandler: PackageAccessedHandler?,
        customCapabilities: Map<ModuleDescriptor.Capability<*>, Any?>
    ) = createDescriptorOptionalBuiltIns(
        library,
        languageVersionSettings,
        storageManager,
        builtIns,
        packageAccessedHandler,
        customCapabilities
    )

    override fun createDescriptorAndNewBuiltIns(
        library: KonanLibrary,
        languageVersionSettings: LanguageVersionSettings,
        storageManager: StorageManager,
        packageAccessedHandler: PackageAccessedHandler?,
        customCapabilities: Map<ModuleDescriptor.Capability<*>, Any?>
    ) = createDescriptorOptionalBuiltIns(library, languageVersionSettings, storageManager, null, packageAccessedHandler, customCapabilities)

    private fun createDescriptorOptionalBuiltIns(
        library: KonanLibrary,
        languageVersionSettings: LanguageVersionSettings,
        storageManager: StorageManager,
        builtIns: KotlinBuiltIns?,
        packageAccessedHandler: PackageAccessedHandler?,
        customCapabilities: Map<ModuleDescriptor.Capability<*>, Any?>
    ): ModuleDescriptorImpl {

        val libraryProto = library.moduleHeaderData

        val moduleName = Name.special(libraryProto.moduleName)
        val moduleOrigin = DeserializedKonanModuleOrigin(library)

        val moduleDescriptor = if (builtIns != null)
            descriptorFactory.createDescriptor(moduleName, storageManager, builtIns, moduleOrigin, customCapabilities)
        else
            descriptorFactory.createDescriptorAndNewBuiltIns(moduleName, storageManager, moduleOrigin, customCapabilities)

        val deserializationConfiguration = CompilerDeserializationConfiguration(languageVersionSettings)

        val provider = packageFragmentsFactory.createPackageFragmentProvider(
            library,
            packageAccessedHandler,
            libraryProto.packageFragmentNameList,
            storageManager,
            moduleDescriptor,
            deserializationConfiguration
        )

        moduleDescriptor.initialize(provider)

        return moduleDescriptor
    }
}
