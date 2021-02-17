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
import org.jetbrains.kotlin.konan.library.KONAN_STDLIB_NAME
import org.jetbrains.kotlin.library.SerializedMetadata
import org.jetbrains.kotlin.library.metadata.parseModuleHeader
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.utils.addIfNotNull
import java.io.File

internal abstract class NativeDistributionModulesProvider(libraries: Collection<NativeLibrary>) : ModulesProvider {
    internal class NativeModuleInfo(
        name: String,
        originalLocation: File,
        val dependencies: Set<String>,
        cInteropAttributes: CInteropModuleAttributes?
    ) : ModuleInfo(name, originalLocation, cInteropAttributes)

    protected val libraryMap: Map<String, NativeLibrary>
    protected val moduleInfoMap: Map<String, NativeModuleInfo>

    init {
        val libraryMap = mutableMapOf<String, NativeLibrary>()
        val moduleInfoMap = mutableMapOf<String, NativeModuleInfo>()

        libraries.forEach { library ->
            val manifestData = library.manifestData

            val name = manifestData.uniqueName
            val location = File(library.library.libraryFile.path)
            val dependencies = manifestData.dependencies.toSet()

            val cInteropAttributes = if (manifestData.isInterop) {
                CInteropModuleAttributes(manifestData.exportForwardDeclarations)
            } else null

            libraryMap.put(name, library)?.let { error("Duplicated libraries: $name") }
            moduleInfoMap[name] = NativeModuleInfo(name, location, dependencies, cInteropAttributes)
        }

        this.libraryMap = libraryMap
        this.moduleInfoMap = moduleInfoMap
    }

    final override fun loadModuleInfos(): Collection<ModuleInfo> = moduleInfoMap.values

    final override fun loadModuleMetadata(name: String): SerializedMetadata {
        val library = libraryMap[name]?.library ?: error("No such library: $name")

        val moduleHeader = library.moduleHeaderData
        val fragmentNames = parseModuleHeader(moduleHeader).packageFragmentNameList.toSet()
        val fragments = fragmentNames.map { fragmentName ->
            val partNames = library.packageMetadataParts(fragmentName)
            partNames.map { partName -> library.packageMetadata(fragmentName, partName) }
        }

        return SerializedMetadata(
            module = moduleHeader,
            fragments = fragments,
            fragmentNames = fragmentNames.toList()
        )
    }

    companion object {
        fun forStandardLibrary(
            storageManager: StorageManager,
            stdlib: NativeLibrary
        ): ModulesProvider {
            check(stdlib.manifestData.uniqueName == KONAN_STDLIB_NAME)

            return object : NativeDistributionModulesProvider(listOf(stdlib)) {
                override fun loadModules(dependencies: Collection<ModuleDescriptor>): Map<String, ModuleDescriptor> {
                    check(dependencies.isEmpty())

                    val stdlibModule = NativeFactories.DefaultDeserializedDescriptorFactory.createDescriptorAndNewBuiltIns(
                        library = stdlib.library,
                        languageVersionSettings = LanguageVersionSettingsImpl.DEFAULT,
                        storageManager = storageManager,
                        packageAccessHandler = null
                    ).apply {
                        setDependencies(listOf(this))
                    }

                    return mapOf(KONAN_STDLIB_NAME to stdlibModule)
                }
            }
        }

        fun platformLibraries(
            storageManager: StorageManager,
            librariesToCommonize: NativeLibrariesToCommonize
        ): ModulesProvider = object : NativeDistributionModulesProvider(librariesToCommonize.libraries) {
            override fun loadModules(dependencies: Collection<ModuleDescriptor>): Map<String, ModuleDescriptor> {
                check(dependencies.isNotEmpty()) { "At least Kotlin/Native stdlib should be provided" }

                val dependenciesMap = mutableMapOf<String, MutableList<ModuleDescriptorImpl>>()
                dependencies.forEach { dependency ->
                    val name = dependency.name.strip()
                    dependenciesMap.getOrPut(name) { mutableListOf() } += dependency as ModuleDescriptorImpl
                }

                val builtIns = dependencies.first().builtIns

                val platformModulesMap = libraryMap.mapValues { (_, library) ->
                    NativeFactories.DefaultDeserializedDescriptorFactory.createDescriptorOptionalBuiltIns(
                        library = library.library,
                        languageVersionSettings = LanguageVersionSettingsImpl.DEFAULT,
                        storageManager = storageManager,
                        builtIns = builtIns,
                        packageAccessHandler = null,
                        lookupTracker = LookupTracker.DO_NOTHING
                    )
                }

                val forwardDeclarations = createKotlinNativeForwardDeclarationsModule(
                    storageManager = storageManager,
                    builtIns = builtIns
                )

                platformModulesMap.forEach { (name, module) ->
                    val moduleDependencies = mutableListOf<ModuleDescriptorImpl>()
                    moduleDependencies += module

                    moduleInfoMap.getValue(name).dependencies.forEach {
                        moduleDependencies.addIfNotNull(platformModulesMap[it])
                        moduleDependencies += dependenciesMap[it].orEmpty()
                    }

                    moduleDependencies += forwardDeclarations

                    module.setDependencies(moduleDependencies)
                }

                return platformModulesMap
            }
        }
    }
}
