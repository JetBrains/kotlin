/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.konan

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.commonizer.BuiltInsProvider
import org.jetbrains.kotlin.descriptors.commonizer.ModulesProvider
import org.jetbrains.kotlin.descriptors.commonizer.ModulesProvider.CInteropModuleAttributes
import org.jetbrains.kotlin.descriptors.commonizer.ModulesProvider.ModuleInfo
import org.jetbrains.kotlin.descriptors.commonizer.utils.NativeFactories
import org.jetbrains.kotlin.descriptors.commonizer.utils.createKotlinNativeForwardDeclarationsModule
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.konan.library.KONAN_STDLIB_NAME
import org.jetbrains.kotlin.storage.StorageManager
import java.io.File

internal class NativeDistributionModulesProvider(
    private val storageManager: StorageManager,
    private val libraries: NativeDistributionLibraries
) : BuiltInsProvider, ModulesProvider {
    override fun loadBuiltIns(): KotlinBuiltIns {
        val stdlib = NativeFactories.DefaultDeserializedDescriptorFactory.createDescriptorAndNewBuiltIns(
            library = libraries.stdlib.library,
            languageVersionSettings = LanguageVersionSettingsImpl.DEFAULT,
            storageManager = storageManager,
            packageAccessHandler = null
        )
        stdlib.setDependencies(listOf(stdlib))

        return stdlib.builtIns
    }

    override fun loadModuleInfos(): Map<String, ModuleInfo> {
        return libraries.platformLibs.associate { library ->
            val manifestData = library.manifestData

            val name = manifestData.uniqueName
            val location = File(library.library.libraryFile.path)

            val cInteropAttributes = if (manifestData.isInterop) {
                val packageFqName = manifestData.packageFqName
                    ?: manifestData.shortName?.let { "platform.$it" }
                    ?: manifestData.uniqueName.substringAfter("platform.").let { "platform.$it" }

                CInteropModuleAttributes(packageFqName, manifestData.exportForwardDeclarations)
            } else null

            name to ModuleInfo(name, location, cInteropAttributes)
        }
    }

    override fun loadModules(): Map<String, ModuleDescriptor> {
        val builtIns = loadBuiltIns()
        val stdlib = builtIns.builtInsModule

        val platformModulesMap = libraries.platformLibs.associate { library ->
            val name = library.manifestData.uniqueName
            val module = NativeFactories.DefaultDeserializedDescriptorFactory.createDescriptorOptionalBuiltIns(
                library = library.library,
                languageVersionSettings = LanguageVersionSettingsImpl.DEFAULT,
                storageManager = storageManager,
                builtIns = builtIns,
                packageAccessHandler = null,
                lookupTracker = LookupTracker.DO_NOTHING
            )

            name to module
        }

        val forwardDeclarations = createKotlinNativeForwardDeclarationsModule(
            storageManager = storageManager,
            builtIns = builtIns
        )

        platformModulesMap.forEach { (name, module) ->
            val dependencies = libraries.getManifest(name)
                .dependencies
                .map { if (it == KONAN_STDLIB_NAME) stdlib else platformModulesMap.getValue(it) }

            module.setDependencies(listOf(module) + dependencies + forwardDeclarations)
        }

        return platformModulesMap
    }
}
