/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.konan

import org.jetbrains.kotlin.commonizer.ModulesProvider
import org.jetbrains.kotlin.commonizer.ModulesProvider.ModuleInfo
import org.jetbrains.kotlin.commonizer.konan.DefaultModulesProvider.DuplicateLibraryHandler
import org.jetbrains.kotlin.library.SerializedMetadata
import org.jetbrains.kotlin.library.metadata.parseModuleHeader
import org.jetbrains.kotlin.util.Logger

internal class DefaultModulesProvider private constructor(
    libraries: Collection<NativeLibrary>,
    duplicateLibraryHandler: DuplicateLibraryHandler
) : ModulesProvider {

    internal class NativeModuleInfo(
        name: String,
        val dependencies: Set<String>,
        cInteropAttributes: ModulesProvider.CInteropModuleAttributes?
    ) : ModuleInfo(name, cInteropAttributes)

    private fun interface DuplicateLibraryHandler {
        fun onDuplicateLibrary(name: String)

        companion object {
            val error = DuplicateLibraryHandler { name -> error("Duplicated libraries: $name") }
            fun warning(logger: Logger) = DuplicateLibraryHandler { name -> logger.warning("Duplicated libraries: $name") }
        }
    }

    private val libraryMap: Map<String, NativeLibrary>
    private val moduleInfoMap: Map<String, NativeModuleInfo>

    init {
        val libraryMap = mutableMapOf<String, NativeLibrary>()
        val moduleInfoMap = mutableMapOf<String, NativeModuleInfo>()

        libraries.forEach { library ->
            val manifestData = library.manifestData

            val name = manifestData.uniqueName
            val dependencies = manifestData.dependencies.toSet()

            val cInteropAttributes = if (manifestData.isInterop) {
                val packageFqName = manifestData.packageFqName ?: error("Main package FQ name not specified for module $name")
                ModulesProvider.CInteropModuleAttributes(packageFqName, manifestData.exportForwardDeclarations)
            } else null

            libraryMap.put(name, library)?.let { duplicateLibraryHandler.onDuplicateLibrary(name) }
            moduleInfoMap[name] = NativeModuleInfo(name, dependencies, cInteropAttributes)
        }

        this.libraryMap = libraryMap
        this.moduleInfoMap = moduleInfoMap
    }

    override val moduleInfos: Collection<ModuleInfo> get() = moduleInfoMap.values

    override fun loadModuleMetadata(name: String): SerializedMetadata {
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
        fun create(librariesToCommonize: NativeLibrariesToCommonize): ModulesProvider =
            DefaultModulesProvider(librariesToCommonize.libraries, DuplicateLibraryHandler.error)

        fun forDependencies(libraries: Iterable<NativeLibrary>, logger: Logger): ModulesProvider =
            DefaultModulesProvider(libraries.toList(), DuplicateLibraryHandler.warning(logger))
    }
}
