/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("IncorrectFormatting", "unused")

package org.jetbrains.kotlin.konan.config

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.KonanTarget

object NativeConfigurationKeys {
    @JvmField
    val KONAN_HOME = CompilerConfigurationKey.create<String>("overridden compiler distribution path")

    @JvmField
    val KONAN_LIBRARIES = CompilerConfigurationKey.create<List<String>>("library file paths")

    @JvmField
    val KONAN_FRIEND_LIBRARIES = CompilerConfigurationKey.create<List<String>>("friend library paths")

    @JvmField
    val KONAN_REFINES_MODULES = CompilerConfigurationKey.create<List<String>>("refines module paths")

    @JvmField
    val KONAN_INCLUDED_LIBRARIES = CompilerConfigurationKey.create<List<String>>("klibs processed in the same manner as source files")

    @JvmField
    val KONAN_MANIFEST_ADDEND = CompilerConfigurationKey.create<String>("provide manifest addend file")

    @JvmField
    val KONAN_GENERATED_HEADER_KLIB_PATH = CompilerConfigurationKey.create<String>("path to file where header klib should be produced")

    @JvmField
    val KONAN_NATIVE_LIBRARIES = CompilerConfigurationKey.create<List<String>>("native library file paths")

    @JvmField
    val KONAN_INCLUDED_BINARIES = CompilerConfigurationKey.create<List<String>>("included binary file paths")

    @JvmField
    val KONAN_PRODUCED_ARTIFACT_KIND = CompilerConfigurationKey.create<CompilerOutputKind>("compiler output kind")

    @JvmField
    val KONAN_NO_STDLIB = CompilerConfigurationKey.create<Boolean>("don't link with stdlib")

    @JvmField
    val KONAN_NO_DEFAULT_LIBS = CompilerConfigurationKey.create<Boolean>("don't link with the default libraries")

    @JvmField
    val KONAN_PURGE_USER_LIBS = CompilerConfigurationKey.create<Boolean>("purge user-specified libs too")

    @JvmField
    val KONAN_DONT_COMPRESS_KLIBS = CompilerConfigurationKey.create<Boolean>("don't the library into a klib file")

    @JvmField
    val KONAN_OUTPUT_PATH = CompilerConfigurationKey.create<String>("program or library name")

    @JvmField
    val KONAN_SHORT_MODULE_NAME = CompilerConfigurationKey.create<String>("short module name for IDE and export")

    @JvmField
    val KONAN_TARGET = CompilerConfigurationKey.create<String>("target we compile for")

    @JvmField
    val KONAN_WRITE_DEPENDENCIES_OF_PRODUCED_KLIB_TO = CompilerConfigurationKey.create<String>("write dependencies of the klib being produced to the given path")

    @JvmField
    val KONAN_MANIFEST_NATIVE_TARGETS = CompilerConfigurationKey.create<List<KonanTarget>>("value of native_targets property to write in manifest")

}

var CompilerConfiguration.konanHome: String?
    get() = get(NativeConfigurationKeys.KONAN_HOME)
    set(value) { putIfNotNull(NativeConfigurationKeys.KONAN_HOME, value) }

var CompilerConfiguration.konanLibraries: List<String>
    get() = getList(NativeConfigurationKeys.KONAN_LIBRARIES)
    set(value) { put(NativeConfigurationKeys.KONAN_LIBRARIES, value) }

var CompilerConfiguration.konanFriendLibraries: List<String>
    get() = getList(NativeConfigurationKeys.KONAN_FRIEND_LIBRARIES)
    set(value) { put(NativeConfigurationKeys.KONAN_FRIEND_LIBRARIES, value) }

var CompilerConfiguration.konanRefinesModules: List<String>
    get() = getList(NativeConfigurationKeys.KONAN_REFINES_MODULES)
    set(value) { put(NativeConfigurationKeys.KONAN_REFINES_MODULES, value) }

var CompilerConfiguration.konanIncludedLibraries: List<String>
    get() = getList(NativeConfigurationKeys.KONAN_INCLUDED_LIBRARIES)
    set(value) { put(NativeConfigurationKeys.KONAN_INCLUDED_LIBRARIES, value) }

var CompilerConfiguration.konanManifestAddend: String?
    get() = get(NativeConfigurationKeys.KONAN_MANIFEST_ADDEND)
    set(value) { putIfNotNull(NativeConfigurationKeys.KONAN_MANIFEST_ADDEND, value) }

var CompilerConfiguration.konanGeneratedHeaderKlibPath: String?
    get() = get(NativeConfigurationKeys.KONAN_GENERATED_HEADER_KLIB_PATH)
    set(value) { putIfNotNull(NativeConfigurationKeys.KONAN_GENERATED_HEADER_KLIB_PATH, value) }

var CompilerConfiguration.konanNativeLibraries: List<String>
    get() = getList(NativeConfigurationKeys.KONAN_NATIVE_LIBRARIES)
    set(value) { put(NativeConfigurationKeys.KONAN_NATIVE_LIBRARIES, value) }

var CompilerConfiguration.konanIncludedBinaries: List<String>
    get() = getList(NativeConfigurationKeys.KONAN_INCLUDED_BINARIES)
    set(value) { put(NativeConfigurationKeys.KONAN_INCLUDED_BINARIES, value) }

var CompilerConfiguration.konanProducedArtifactKind: CompilerOutputKind?
    get() = get(NativeConfigurationKeys.KONAN_PRODUCED_ARTIFACT_KIND)
    set(value) { put(NativeConfigurationKeys.KONAN_PRODUCED_ARTIFACT_KIND, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.konanNoStdlib: Boolean
    get() = getBoolean(NativeConfigurationKeys.KONAN_NO_STDLIB)
    set(value) { put(NativeConfigurationKeys.KONAN_NO_STDLIB, value) }

var CompilerConfiguration.konanNoDefaultLibs: Boolean
    get() = getBoolean(NativeConfigurationKeys.KONAN_NO_DEFAULT_LIBS)
    set(value) { put(NativeConfigurationKeys.KONAN_NO_DEFAULT_LIBS, value) }

var CompilerConfiguration.konanPurgeUserLibs: Boolean
    get() = getBoolean(NativeConfigurationKeys.KONAN_PURGE_USER_LIBS)
    set(value) { put(NativeConfigurationKeys.KONAN_PURGE_USER_LIBS, value) }

var CompilerConfiguration.konanDontCompressKlibs: Boolean
    get() = getBoolean(NativeConfigurationKeys.KONAN_DONT_COMPRESS_KLIBS)
    set(value) { put(NativeConfigurationKeys.KONAN_DONT_COMPRESS_KLIBS, value) }

var CompilerConfiguration.konanOutputPath: String?
    get() = get(NativeConfigurationKeys.KONAN_OUTPUT_PATH)
    set(value) { putIfNotNull(NativeConfigurationKeys.KONAN_OUTPUT_PATH, value) }

var CompilerConfiguration.konanShortModuleName: String?
    get() = get(NativeConfigurationKeys.KONAN_SHORT_MODULE_NAME)
    set(value) { put(NativeConfigurationKeys.KONAN_SHORT_MODULE_NAME, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.konanTarget: String?
    get() = get(NativeConfigurationKeys.KONAN_TARGET)
    set(value) { putIfNotNull(NativeConfigurationKeys.KONAN_TARGET, value) }

var CompilerConfiguration.konanWriteDependenciesOfProducedKlibTo: String?
    get() = get(NativeConfigurationKeys.KONAN_WRITE_DEPENDENCIES_OF_PRODUCED_KLIB_TO)
    set(value) { putIfNotNull(NativeConfigurationKeys.KONAN_WRITE_DEPENDENCIES_OF_PRODUCED_KLIB_TO, value) }

var CompilerConfiguration.konanManifestNativeTargets: List<KonanTarget>
    get() = getList(NativeConfigurationKeys.KONAN_MANIFEST_NATIVE_TARGETS)
    set(value) { put(NativeConfigurationKeys.KONAN_MANIFEST_NATIVE_TARGETS, value) }

