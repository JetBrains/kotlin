/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.konan.impl

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.descriptors.konan.KonanModuleDescriptorFactory
import org.jetbrains.kotlin.descriptors.konan.KonanModuleOrigin
import org.jetbrains.kotlin.descriptors.konan.isInteropLibrary
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.ImplicitIntegerCoercion
import org.jetbrains.kotlin.storage.StorageManager

internal object KonanModuleDescriptorFactoryImpl : KonanModuleDescriptorFactory {

    override fun createDescriptor(
        name: Name,
        storageManager: StorageManager,
        builtIns: KotlinBuiltIns,
        origin: KonanModuleOrigin,
        customCapabilities: Map<ModuleDescriptor.Capability<*>, Any?>
    ) = ModuleDescriptorImpl(
        name,
        storageManager,
        builtIns,
        capabilities = customCapabilities + mapOf(
            KonanModuleOrigin.CAPABILITY to origin,
            ImplicitIntegerCoercion.MODULE_CAPABILITY to origin.isInteropLibrary()
        )
    )

    override fun createDescriptorAndNewBuiltIns(
        name: Name,
        storageManager: StorageManager,
        origin: KonanModuleOrigin,
        customCapabilities: Map<ModuleDescriptor.Capability<*>, Any?>
    ): ModuleDescriptorImpl {

        val builtIns = KonanBuiltIns(storageManager)

        val moduleDescriptor = createDescriptor(name, storageManager, builtIns, origin, customCapabilities)
        builtIns.builtInsModule = moduleDescriptor

        return moduleDescriptor
    }
}
