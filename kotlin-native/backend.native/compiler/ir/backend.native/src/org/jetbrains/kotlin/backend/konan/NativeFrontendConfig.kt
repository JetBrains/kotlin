/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.konan.properties.loadProperties
import org.jetbrains.kotlin.konan.util.visibleName
import org.jetbrains.kotlin.library.metadata.resolver.TopologicalLibraryOrder
import org.jetbrains.kotlin.util.removeSuffixIfPresent

class NativeFrontendConfig(private val baseNativeConfig: BaseNativeConfig) {

    val configuration get() = baseNativeConfig.configuration

    internal val target get() = baseNativeConfig.targetManager.target
    internal val resolve get() = baseNativeConfig.resolve

    internal val produce get() = configuration.get(KonanConfigKeys.PRODUCE)!!

    val outputPath get() = configuration.get(KonanConfigKeys.OUTPUT)?.removeSuffixIfPresent(produce.suffix(target)) ?: produce.visibleName

    val moduleId: String
        get() = configuration.get(KonanConfigKeys.MODULE_NAME) ?: implicitModuleName

    val resolvedLibraries get() = resolve.resolvedLibraries

    internal val friendModuleFiles: Set<File> =
            configuration.get(KonanConfigKeys.FRIEND_MODULES)?.map { File(it) }?.toSet() ?: emptySet()

    internal val refinesModuleFiles: Set<File> =
            configuration.get(KonanConfigKeys.REFINES_MODULES)?.map { File(it) }?.toSet().orEmpty()

    internal val languageVersionSettings =
            configuration.get(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS)!!

    internal val metadataKlib get() = configuration.getBoolean(CommonConfigurationKeys.METADATA_KLIB)

    internal val unitSuspendFunctionObjCExport: UnitSuspendFunctionObjCExport
        get() = configuration.get(BinaryOptions.unitSuspendFunctionObjCExport) ?: UnitSuspendFunctionObjCExport.DEFAULT

    val fullExportedNamePrefix: String
        get() = configuration.get(KonanConfigKeys.FULL_EXPORTED_NAME_PREFIX) ?: implicitModuleName

    private val implicitModuleName: String
        get() = File(outputPath).name

    val shortModuleName: String?
        get() = configuration.get(KonanConfigKeys.SHORT_MODULE_NAME)

    internal val manifestProperties = configuration.get(KonanConfigKeys.MANIFEST_FILE)?.let {
        File(it).loadProperties()
    }

    internal val nativeTargetsForManifest = configuration.get(KonanConfigKeys.MANIFEST_NATIVE_TARGETS)

    internal val writeDependenciesOfProducedKlibTo
        get() = configuration.get(KonanConfigKeys.WRITE_DEPENDENCIES_OF_PRODUCED_KLIB_TO)

    internal val nativeLibraries: List<String> =
            configuration.getList(KonanConfigKeys.NATIVE_LIBRARY_FILES)

    internal val includeBinaries: List<String> =
            configuration.getList(KonanConfigKeys.INCLUDED_BINARY_FILES)

    internal val purgeUserLibs: Boolean
        get() = configuration.getBoolean(KonanConfigKeys.PURGE_USER_LIBS)

    fun librariesWithDependencies(): List<KonanLibrary> {
        return resolvedLibraries.filterRoots { (!it.isDefault && !this.purgeUserLibs) || it.isNeededForLink }.getFullList(TopologicalLibraryOrder).map { it as KonanLibrary }
    }

    internal val headerKlibPath get() = configuration.get(KonanConfigKeys.HEADER_KLIB)?.removeSuffixIfPresent(".klib")
}