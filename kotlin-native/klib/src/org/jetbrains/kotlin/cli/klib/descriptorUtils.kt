/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.klib

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ModuleCapability
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.metadata.DeserializedKlibModuleOrigin
import org.jetbrains.kotlin.library.metadata.KlibModuleOrigin
import org.jetbrains.kotlin.library.uniqueName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.storage.LockBasedStorageManager

internal fun createFakeModuleDescriptor(library: KotlinLibrary): ModuleDescriptorImpl = createFakeModuleDescriptor(
        moduleName = Name.special("<${library.uniqueName}>"),
        capabilities = mapOf(KlibModuleOrigin.CAPABILITY to DeserializedKlibModuleOrigin(library)),
)

/** A module descriptor for [EmulatedStandardLibraryModuleDeserializer], which has no Klib. */
internal fun createEmulatedStandardLibraryModuleDescriptor(): ModuleDescriptorImpl = createFakeModuleDescriptor(
        moduleName = EMULATED_STDLIB_MODULE_NAME,
        capabilities = emptyMap(),
)

private fun createFakeModuleDescriptor(
        moduleName: Name,
        capabilities: Map<ModuleCapability<*>, Any?>,
): ModuleDescriptorImpl {
    val builtIns = object : KotlinBuiltIns(LockBasedStorageManager.NO_LOCKS) {}
    val moduleDescriptor = ModuleDescriptorImpl(
            moduleName = moduleName,
            storageManager = LockBasedStorageManager.NO_LOCKS,
            builtIns = builtIns,
            capabilities = capabilities,
    )
    moduleDescriptor.setDependencies(moduleDescriptor)
    builtIns.builtInsModule = moduleDescriptor
    return moduleDescriptor
}

internal val EMULATED_STDLIB_MODULE_NAME = Name.special("<emulated stdlib>")
