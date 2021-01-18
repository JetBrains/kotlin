/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.konan

import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.commonizer.ModulesProvider
import org.jetbrains.kotlin.descriptors.commonizer.ModulesProvider.CInteropModuleAttributes
import org.jetbrains.kotlin.descriptors.commonizer.ModulesProvider.ModuleInfo
import org.jetbrains.kotlin.descriptors.commonizer.utils.NativeFactories
import org.jetbrains.kotlin.descriptors.commonizer.utils.createKotlinNativeForwardDeclarationsModule
import org.jetbrains.kotlin.descriptors.commonizer.utils.strip
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.utils.addIfNotNull
import java.io.File

internal class NativeDistributionModulesProvider(
    private val storageManager: StorageManager,
    private val librariesToCommonize: NativeLibrariesToCommonize
) : ModulesProvider {
    override fun loadModuleInfos(): Map<String, ModuleInfo> {
        return librariesToCommonize.libraries.associate { library ->
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

    override fun loadModules(dependencies: Collection<ModuleDescriptor>): Map<String, ModuleDescriptor> {
        check(dependencies.isNotEmpty()) { "At least Kotlin/Native stdlib should be provided" }

        val dependenciesMap = mutableMapOf<String, MutableList<ModuleDescriptorImpl>>()
        dependencies.forEach { dependency ->
            val name = dependency.name.strip()
            dependenciesMap.getOrPut(name) { mutableListOf() } += dependency as ModuleDescriptorImpl
        }

        val builtIns = dependencies.first().builtIns

        val platformModulesMap = librariesToCommonize.libraries.associate { library ->
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
            val moduleDependencies = mutableListOf<ModuleDescriptorImpl>()
            moduleDependencies += module

            librariesToCommonize.getManifest(name).dependencies.forEach {
                moduleDependencies.addIfNotNull(platformModulesMap[it])
                moduleDependencies += dependenciesMap[it].orEmpty()
            }

            moduleDependencies += forwardDeclarations

            module.setDependencies(moduleDependencies)
        }

        return platformModulesMap
    }
}
