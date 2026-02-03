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

import org.jetbrains.kotlin.backend.konan.AllocationMode
import org.jetbrains.kotlin.backend.konan.LlvmVariant
import org.jetbrains.kotlin.backend.konan.TestRunnerKind
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.KonanTarget

object NativeConfigurationKeys {
    // Bundle ID to be set in Info.plist of a produced framework.
    @JvmField
    val BUNDLE_ID = CompilerConfigurationKey.create<String>("BUNDLE_ID")

    // Check dependencies and download the missing ones.
    @JvmField
    val CHECK_DEPENDENCIES = CompilerConfigurationKey.create<Boolean>("CHECK_DEPENDENCIES")

    @JvmField
    val DEBUG = CompilerConfigurationKey.create<Boolean>("DEBUG")

    @JvmField
    val FAKE_OVERRIDE_VALIDATOR = CompilerConfigurationKey.create<Boolean>("FAKE_OVERRIDE_VALIDATOR")

    @JvmField
    val EMIT_LAZY_OBJC_HEADER_FILE = CompilerConfigurationKey.create<String>("EMIT_LAZY_OBJC_HEADER_FILE")

    @JvmField
    val ENABLE_ASSERTIONS = CompilerConfigurationKey.create<Boolean>("ENABLE_ASSERTIONS")

    // Fully qualified main() name.
    @JvmField
    val ENTRY = CompilerConfigurationKey.create<String>("ENTRY")

    // Libraries included into produced framework API.
    @JvmField
    val EXPORTED_LIBRARIES = CompilerConfigurationKey.create<List<String>>("EXPORTED_LIBRARIES")

    // Prefix used when exporting Kotlin names to other languages.
    @JvmField
    val FULL_EXPORTED_NAME_PREFIX = CompilerConfigurationKey.create<String>("FULL_EXPORTED_NAME_PREFIX")

    @JvmField
    val LIBRARY_TO_ADD_TO_CACHE = CompilerConfigurationKey.create<String>("LIBRARY_TO_ADD_TO_CACHE")

    @JvmField
    val CACHE_DIRECTORIES = CompilerConfigurationKey.create<List<String>>("CACHE_DIRECTORIES")

    // Paths to the root directories from which dependencies are to be cached automatically.
    @JvmField
    val AUTO_CACHEABLE_FROM = CompilerConfigurationKey.create<List<String>>("AUTO_CACHEABLE_FROM")

    // Path to the directory where to put caches for auto-cacheable dependencies.
    @JvmField
    val AUTO_CACHE_DIR = CompilerConfigurationKey.create<String>("AUTO_CACHE_DIR")

    // Path to the directory where to put incremental build caches.
    @JvmField
    val INCREMENTAL_CACHE_DIR = CompilerConfigurationKey.create<String>("INCREMENTAL_CACHE_DIR")

    // Mapping from library paths to cache paths.
    @JvmField
    val CACHED_LIBRARIES = CompilerConfigurationKey.create<Map<String, String>>("CACHED_LIBRARIES")

    // Which files should be compiled to cache.
    @JvmField
    val FILES_TO_CACHE = CompilerConfigurationKey.create<List<String>>("FILES_TO_CACHE")

    @JvmField
    val MAKE_PER_FILE_CACHE = CompilerConfigurationKey.create<Boolean>("MAKE_PER_FILE_CACHE")

    @JvmField
    val FRAMEWORK_IMPORT_HEADERS = CompilerConfigurationKey.create<List<String>>("FRAMEWORK_IMPORT_HEADERS")

    @JvmField
    val KONAN_FRIEND_LIBRARIES = CompilerConfigurationKey.create<List<String>>("KONAN_FRIEND_LIBRARIES")

    @JvmField
    val KONAN_REFINES_MODULES = CompilerConfigurationKey.create<List<String>>("KONAN_REFINES_MODULES")

    @JvmField
    val GENERATE_TEST_RUNNER = CompilerConfigurationKey.create<TestRunnerKind>("GENERATE_TEST_RUNNER")

    @JvmField
    val KONAN_INCLUDED_BINARIES = CompilerConfigurationKey.create<List<String>>("KONAN_INCLUDED_BINARIES")

    @JvmField
    val KONAN_HOME = CompilerConfigurationKey.create<String>("KONAN_HOME")

    @JvmField
    val KONAN_LIBRARIES = CompilerConfigurationKey.create<List<String>>("KONAN_LIBRARIES")

    @JvmField
    val LIGHT_DEBUG = CompilerConfigurationKey.create<Boolean>("LIGHT_DEBUG")

    // Generates debug trampolines to make debugger breakpoint resolution more accurate.
    @JvmField
    val GENERATE_DEBUG_TRAMPOLINE = CompilerConfigurationKey.create<Boolean>("GENERATE_DEBUG_TRAMPOLINE")

    @JvmField
    val LINKER_ARGS = CompilerConfigurationKey.create<List<String>>("LINKER_ARGS")

    @JvmField
    val LIST_TARGETS = CompilerConfigurationKey.create<Boolean>("LIST_TARGETS")

    @JvmField
    val KONAN_MANIFEST_ADDEND = CompilerConfigurationKey.create<String>("KONAN_MANIFEST_ADDEND")

    // Path to file where header klib should be produced.
    @JvmField
    val KONAN_GENERATED_HEADER_KLIB_PATH = CompilerConfigurationKey.create<String>("KONAN_GENERATED_HEADER_KLIB_PATH")

    @JvmField
    val KONAN_NATIVE_LIBRARIES = CompilerConfigurationKey.create<List<String>>("KONAN_NATIVE_LIBRARIES")

    // Don't link with the default libraries.
    @JvmField
    val KONAN_NO_DEFAULT_LIBS = CompilerConfigurationKey.create<Boolean>("KONAN_NO_DEFAULT_LIBS")

    // Don't link with the endorsed libraries.
    @JvmField
    val KONAN_NO_ENDORSED_LIBS = CompilerConfigurationKey.create<Boolean>("KONAN_NO_ENDORSED_LIBS")

    // Assume 'main' entry point to be provided by external libraries.
    @JvmField
    val NOMAIN = CompilerConfigurationKey.create<Boolean>("NOMAIN")

    // Don't link with stdlib.
    @JvmField
    val KONAN_NO_STDLIB = CompilerConfigurationKey.create<Boolean>("KONAN_NO_STDLIB")

    // Don't pack the library into a klib file.
    @JvmField
    val KONAN_DONT_COMPRESS_KLIB = CompilerConfigurationKey.create<Boolean>("KONAN_DONT_COMPRESS_KLIB")

    @JvmField
    val OPTIMIZATION = CompilerConfigurationKey.create<Boolean>("OPTIMIZATION")

    @JvmField
    val KONAN_OUTPUT_PATH = CompilerConfigurationKey.create<String>("KONAN_OUTPUT_PATH")

    @JvmField
    val OVERRIDE_CLANG_OPTIONS = CompilerConfigurationKey.create<List<String>>("OVERRIDE_CLANG_OPTIONS")

    @JvmField
    val ALLOCATION_MODE = CompilerConfigurationKey.create<AllocationMode>("ALLOCATION_MODE")

    // Export KDoc into klib and framework.
    @JvmField
    val EXPORT_KDOC = CompilerConfigurationKey.create<Boolean>("EXPORT_KDOC")

    @JvmField
    val PRINT_BITCODE = CompilerConfigurationKey.create<Boolean>("PRINT_BITCODE")

    @JvmField
    val PRINT_IR = CompilerConfigurationKey.create<Boolean>("PRINT_IR")

    @JvmField
    val PRINT_FILES = CompilerConfigurationKey.create<Boolean>("PRINT_FILES")

    @JvmField
    val KONAN_PRODUCED_ARTIFACT_KIND = CompilerConfigurationKey.create<CompilerOutputKind>("KONAN_PRODUCED_ARTIFACT_KIND")

    @JvmField
    val PURGE_USER_LIBS = CompilerConfigurationKey.create<Boolean>("PURGE_USER_LIBS")

    @JvmField
    val RUNTIME_FILE = CompilerConfigurationKey.create<String>("RUNTIME_FILE")

    // Klibs processed in the same manner as source files.
    @JvmField
    val INCLUDED_LIBRARIES = CompilerConfigurationKey.create<List<String>>("INCLUDED_LIBRARIES")

    // Short module name for IDE and export.
    @JvmField
    val SHORT_MODULE_NAME = CompilerConfigurationKey.create<String>("SHORT_MODULE_NAME")

    // Produce a static library for a framework.
    @JvmField
    val STATIC_FRAMEWORK = CompilerConfigurationKey.create<Boolean>("STATIC_FRAMEWORK")

    @JvmField
    val TARGET = CompilerConfigurationKey.create<String>("TARGET")

    @JvmField
    val TEMPORARY_FILES_DIR = CompilerConfigurationKey.create<String>("TEMPORARY_FILES_DIR")

    @JvmField
    val SAVE_LLVM_IR = CompilerConfigurationKey.create<List<String>>("SAVE_LLVM_IR")

    @JvmField
    val VERIFY_BITCODE = CompilerConfigurationKey.create<Boolean>("VERIFY_BITCODE")

    @JvmField
    val VERIFY_COMPILER = CompilerConfigurationKey.create<Boolean>("VERIFY_COMPILER")

    @JvmField
    val WRITE_DEPENDENCIES_OF_PRODUCED_KLIB_TO = CompilerConfigurationKey.create<String>("WRITE_DEPENDENCIES_OF_PRODUCED_KLIB_TO")

    @JvmField
    val DEBUG_INFO_VERSION = CompilerConfigurationKey.create<Int>("DEBUG_INFO_VERSION")

    // Write objc header with generics support.
    @JvmField
    val OBJC_GENERICS = CompilerConfigurationKey.create<Boolean>("OBJC_GENERICS")

    // Remap file source paths in debug info.
    @JvmField
    val DEBUG_PREFIX_MAP = CompilerConfigurationKey.create<Map<String, String>>("DEBUG_PREFIX_MAP")

    // Perform compiler caches pre-link.
    @JvmField
    val PRE_LINK_CACHES = CompilerConfigurationKey.create<Boolean>("PRE_LINK_CACHES")

    // Override konan.properties values.
    @JvmField
    val OVERRIDE_KONAN_PROPERTIES = CompilerConfigurationKey.create<Map<String, String>>("OVERRIDE_KONAN_PROPERTIES")

    @JvmField
    val PROPERTY_LAZY_INITIALIZATION = CompilerConfigurationKey.create<Boolean>("PROPERTY_LAZY_INITIALIZATION")

    // Use external dependencies to enhance IR linker error messages.
    @JvmField
    val EXTERNAL_DEPENDENCIES = CompilerConfigurationKey.create<String>("EXTERNAL_DEPENDENCIES")

    @JvmField
    val LLVM_VARIANT = CompilerConfigurationKey.create<LlvmVariant>("LLVM_VARIANT")

    @JvmField
    val RUNTIME_LOGS = CompilerConfigurationKey.create<String>("RUNTIME_LOGS")

    // Path to a file to dump the list of all available tests.
    @JvmField
    val TEST_DUMP_OUTPUT_PATH = CompilerConfigurationKey.create<String>("TEST_DUMP_OUTPUT_PATH")

    // Do not generate binary in framework.
    @JvmField
    val OMIT_FRAMEWORK_BINARY = CompilerConfigurationKey.create<Boolean>("OMIT_FRAMEWORK_BINARY")

    // Path to bitcode file to compile.
    @JvmField
    val COMPILE_FROM_BITCODE = CompilerConfigurationKey.create<String>("COMPILE_FROM_BITCODE")

    // Path to serialized dependencies for native linking.
    @JvmField
    val SERIALIZED_DEPENDENCIES = CompilerConfigurationKey.create<String>("SERIALIZED_DEPENDENCIES")

    // Path to save serialized dependencies to.
    @JvmField
    val SAVE_DEPENDENCIES_PATH = CompilerConfigurationKey.create<String>("SAVE_DEPENDENCIES_PATH")

    // Directory to store LLVM IR from phases.
    @JvmField
    val SAVE_LLVM_IR_DIRECTORY = CompilerConfigurationKey.create<String>("SAVE_LLVM_IR_DIRECTORY")

    // Directory for storing konan dependencies, cache and prebuilds.
    @JvmField
    val KONAN_DATA_DIR = CompilerConfigurationKey.create<String>("KONAN_DATA_DIR")

    // Value of native_targets property to write in manifest.
    @JvmField
    val MANIFEST_NATIVE_TARGETS = CompilerConfigurationKey.create<List<KonanTarget>>("MANIFEST_NATIVE_TARGETS")

    // LLVM passes to run instead of module optimization pipeline.
    @JvmField
    val LLVM_MODULE_PASSES = CompilerConfigurationKey.create<String>("LLVM_MODULE_PASSES")

    // LLVM passes to run instead of LTO optimization pipeline.
    @JvmField
    val LLVM_LTO_PASSES = CompilerConfigurationKey.create<String>("LLVM_LTO_PASSES")

}

var CompilerConfiguration.bundleId: String?
    get() = get(NativeConfigurationKeys.BUNDLE_ID)
    set(value) { put(NativeConfigurationKeys.BUNDLE_ID, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.checkDependencies: Boolean
    get() = getBoolean(NativeConfigurationKeys.CHECK_DEPENDENCIES)
    set(value) { put(NativeConfigurationKeys.CHECK_DEPENDENCIES, value) }

var CompilerConfiguration.debug: Boolean
    get() = getBoolean(NativeConfigurationKeys.DEBUG)
    set(value) { put(NativeConfigurationKeys.DEBUG, value) }

var CompilerConfiguration.fakeOverrideValidator: Boolean
    get() = getBoolean(NativeConfigurationKeys.FAKE_OVERRIDE_VALIDATOR)
    set(value) { put(NativeConfigurationKeys.FAKE_OVERRIDE_VALIDATOR, value) }

var CompilerConfiguration.emitLazyObjcHeaderFile: String?
    get() = get(NativeConfigurationKeys.EMIT_LAZY_OBJC_HEADER_FILE)
    set(value) { put(NativeConfigurationKeys.EMIT_LAZY_OBJC_HEADER_FILE, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.enableAssertions: Boolean
    get() = getBoolean(NativeConfigurationKeys.ENABLE_ASSERTIONS)
    set(value) { put(NativeConfigurationKeys.ENABLE_ASSERTIONS, value) }

var CompilerConfiguration.entry: String?
    get() = get(NativeConfigurationKeys.ENTRY)
    set(value) { put(NativeConfigurationKeys.ENTRY, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.exportedLibraries: List<String>
    get() = getList(NativeConfigurationKeys.EXPORTED_LIBRARIES)
    set(value) { put(NativeConfigurationKeys.EXPORTED_LIBRARIES, value) }

var CompilerConfiguration.fullExportedNamePrefix: String?
    get() = get(NativeConfigurationKeys.FULL_EXPORTED_NAME_PREFIX)
    set(value) { put(NativeConfigurationKeys.FULL_EXPORTED_NAME_PREFIX, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.libraryToAddToCache: String?
    get() = get(NativeConfigurationKeys.LIBRARY_TO_ADD_TO_CACHE)
    set(value) { put(NativeConfigurationKeys.LIBRARY_TO_ADD_TO_CACHE, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.cacheDirectories: List<String>
    get() = getList(NativeConfigurationKeys.CACHE_DIRECTORIES)
    set(value) { put(NativeConfigurationKeys.CACHE_DIRECTORIES, value) }

var CompilerConfiguration.autoCacheableFrom: List<String>
    get() = getList(NativeConfigurationKeys.AUTO_CACHEABLE_FROM)
    set(value) { put(NativeConfigurationKeys.AUTO_CACHEABLE_FROM, value) }

var CompilerConfiguration.autoCacheDir: String?
    get() = get(NativeConfigurationKeys.AUTO_CACHE_DIR)
    set(value) { put(NativeConfigurationKeys.AUTO_CACHE_DIR, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.incrementalCacheDir: String?
    get() = get(NativeConfigurationKeys.INCREMENTAL_CACHE_DIR)
    set(value) { put(NativeConfigurationKeys.INCREMENTAL_CACHE_DIR, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.cachedLibraries: Map<String, String>
    get() = getMap(NativeConfigurationKeys.CACHED_LIBRARIES)
    set(value) { put(NativeConfigurationKeys.CACHED_LIBRARIES, value) }

var CompilerConfiguration.filesToCache: List<String>
    get() = getList(NativeConfigurationKeys.FILES_TO_CACHE)
    set(value) { put(NativeConfigurationKeys.FILES_TO_CACHE, value) }

var CompilerConfiguration.makePerFileCache: Boolean
    get() = getBoolean(NativeConfigurationKeys.MAKE_PER_FILE_CACHE)
    set(value) { put(NativeConfigurationKeys.MAKE_PER_FILE_CACHE, value) }

var CompilerConfiguration.frameworkImportHeaders: List<String>
    get() = getList(NativeConfigurationKeys.FRAMEWORK_IMPORT_HEADERS)
    set(value) { put(NativeConfigurationKeys.FRAMEWORK_IMPORT_HEADERS, value) }

var CompilerConfiguration.konanFriendLibraries: List<String>
    get() = getList(NativeConfigurationKeys.KONAN_FRIEND_LIBRARIES)
    set(value) { put(NativeConfigurationKeys.KONAN_FRIEND_LIBRARIES, value) }

var CompilerConfiguration.konanRefinesModules: List<String>
    get() = getList(NativeConfigurationKeys.KONAN_REFINES_MODULES)
    set(value) { put(NativeConfigurationKeys.KONAN_REFINES_MODULES, value) }

var CompilerConfiguration.generateTestRunner: TestRunnerKind?
    get() = get(NativeConfigurationKeys.GENERATE_TEST_RUNNER)
    set(value) { put(NativeConfigurationKeys.GENERATE_TEST_RUNNER, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.konanIncludedBinaries: List<String>
    get() = getList(NativeConfigurationKeys.KONAN_INCLUDED_BINARIES)
    set(value) { put(NativeConfigurationKeys.KONAN_INCLUDED_BINARIES, value) }

var CompilerConfiguration.konanHome: String?
    get() = get(NativeConfigurationKeys.KONAN_HOME)
    set(value) { put(NativeConfigurationKeys.KONAN_HOME, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.konanLibraries: List<String>
    get() = getList(NativeConfigurationKeys.KONAN_LIBRARIES)
    set(value) { put(NativeConfigurationKeys.KONAN_LIBRARIES, value) }

var CompilerConfiguration.lightDebug: Boolean
    get() = getBoolean(NativeConfigurationKeys.LIGHT_DEBUG)
    set(value) { put(NativeConfigurationKeys.LIGHT_DEBUG, value) }

var CompilerConfiguration.generateDebugTrampoline: Boolean
    get() = getBoolean(NativeConfigurationKeys.GENERATE_DEBUG_TRAMPOLINE)
    set(value) { put(NativeConfigurationKeys.GENERATE_DEBUG_TRAMPOLINE, value) }

var CompilerConfiguration.linkerArgs: List<String>
    get() = getList(NativeConfigurationKeys.LINKER_ARGS)
    set(value) { put(NativeConfigurationKeys.LINKER_ARGS, value) }

var CompilerConfiguration.listTargets: Boolean
    get() = getBoolean(NativeConfigurationKeys.LIST_TARGETS)
    set(value) { put(NativeConfigurationKeys.LIST_TARGETS, value) }

var CompilerConfiguration.konanManifestAddend: String?
    get() = get(NativeConfigurationKeys.KONAN_MANIFEST_ADDEND)
    set(value) { put(NativeConfigurationKeys.KONAN_MANIFEST_ADDEND, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.konanGeneratedHeaderKlibPath: String?
    get() = get(NativeConfigurationKeys.KONAN_GENERATED_HEADER_KLIB_PATH)
    set(value) { put(NativeConfigurationKeys.KONAN_GENERATED_HEADER_KLIB_PATH, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.konanNativeLibraries: List<String>
    get() = getList(NativeConfigurationKeys.KONAN_NATIVE_LIBRARIES)
    set(value) { put(NativeConfigurationKeys.KONAN_NATIVE_LIBRARIES, value) }

var CompilerConfiguration.konanNoDefaultLibs: Boolean
    get() = getBoolean(NativeConfigurationKeys.KONAN_NO_DEFAULT_LIBS)
    set(value) { put(NativeConfigurationKeys.KONAN_NO_DEFAULT_LIBS, value) }

var CompilerConfiguration.konanNoEndorsedLibs: Boolean
    get() = getBoolean(NativeConfigurationKeys.KONAN_NO_ENDORSED_LIBS)
    set(value) { put(NativeConfigurationKeys.KONAN_NO_ENDORSED_LIBS, value) }

var CompilerConfiguration.nomain: Boolean
    get() = getBoolean(NativeConfigurationKeys.NOMAIN)
    set(value) { put(NativeConfigurationKeys.NOMAIN, value) }

var CompilerConfiguration.konanNoStdlib: Boolean
    get() = getBoolean(NativeConfigurationKeys.KONAN_NO_STDLIB)
    set(value) { put(NativeConfigurationKeys.KONAN_NO_STDLIB, value) }

var CompilerConfiguration.konanDontCompressKlib: Boolean
    get() = getBoolean(NativeConfigurationKeys.KONAN_DONT_COMPRESS_KLIB)
    set(value) { put(NativeConfigurationKeys.KONAN_DONT_COMPRESS_KLIB, value) }

var CompilerConfiguration.optimization: Boolean
    get() = getBoolean(NativeConfigurationKeys.OPTIMIZATION)
    set(value) { put(NativeConfigurationKeys.OPTIMIZATION, value) }

var CompilerConfiguration.konanOutputPath: String?
    get() = get(NativeConfigurationKeys.KONAN_OUTPUT_PATH)
    set(value) { put(NativeConfigurationKeys.KONAN_OUTPUT_PATH, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.overrideClangOptions: List<String>
    get() = getList(NativeConfigurationKeys.OVERRIDE_CLANG_OPTIONS)
    set(value) { put(NativeConfigurationKeys.OVERRIDE_CLANG_OPTIONS, value) }

var CompilerConfiguration.allocationMode: AllocationMode?
    get() = get(NativeConfigurationKeys.ALLOCATION_MODE)
    set(value) { put(NativeConfigurationKeys.ALLOCATION_MODE, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.exportKdoc: Boolean
    get() = getBoolean(NativeConfigurationKeys.EXPORT_KDOC)
    set(value) { put(NativeConfigurationKeys.EXPORT_KDOC, value) }

var CompilerConfiguration.printBitcode: Boolean
    get() = getBoolean(NativeConfigurationKeys.PRINT_BITCODE)
    set(value) { put(NativeConfigurationKeys.PRINT_BITCODE, value) }

var CompilerConfiguration.printIr: Boolean
    get() = getBoolean(NativeConfigurationKeys.PRINT_IR)
    set(value) { put(NativeConfigurationKeys.PRINT_IR, value) }

var CompilerConfiguration.printFiles: Boolean
    get() = getBoolean(NativeConfigurationKeys.PRINT_FILES)
    set(value) { put(NativeConfigurationKeys.PRINT_FILES, value) }

var CompilerConfiguration.konanProducedArtifactKind: CompilerOutputKind?
    get() = get(NativeConfigurationKeys.KONAN_PRODUCED_ARTIFACT_KIND)
    set(value) { put(NativeConfigurationKeys.KONAN_PRODUCED_ARTIFACT_KIND, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.purgeUserLibs: Boolean
    get() = getBoolean(NativeConfigurationKeys.PURGE_USER_LIBS)
    set(value) { put(NativeConfigurationKeys.PURGE_USER_LIBS, value) }

var CompilerConfiguration.runtimeFile: String?
    get() = get(NativeConfigurationKeys.RUNTIME_FILE)
    set(value) { put(NativeConfigurationKeys.RUNTIME_FILE, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.includedLibraries: List<String>
    get() = getList(NativeConfigurationKeys.INCLUDED_LIBRARIES)
    set(value) { put(NativeConfigurationKeys.INCLUDED_LIBRARIES, value) }

var CompilerConfiguration.shortModuleName: String?
    get() = get(NativeConfigurationKeys.SHORT_MODULE_NAME)
    set(value) { put(NativeConfigurationKeys.SHORT_MODULE_NAME, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.staticFramework: Boolean
    get() = getBoolean(NativeConfigurationKeys.STATIC_FRAMEWORK)
    set(value) { put(NativeConfigurationKeys.STATIC_FRAMEWORK, value) }

var CompilerConfiguration.target: String?
    get() = get(NativeConfigurationKeys.TARGET)
    set(value) { put(NativeConfigurationKeys.TARGET, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.temporaryFilesDir: String?
    get() = get(NativeConfigurationKeys.TEMPORARY_FILES_DIR)
    set(value) { put(NativeConfigurationKeys.TEMPORARY_FILES_DIR, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.saveLlvmIr: List<String>
    get() = getList(NativeConfigurationKeys.SAVE_LLVM_IR)
    set(value) { put(NativeConfigurationKeys.SAVE_LLVM_IR, value) }

var CompilerConfiguration.verifyBitcode: Boolean
    get() = getBoolean(NativeConfigurationKeys.VERIFY_BITCODE)
    set(value) { put(NativeConfigurationKeys.VERIFY_BITCODE, value) }

var CompilerConfiguration.verifyCompiler: Boolean
    get() = getBoolean(NativeConfigurationKeys.VERIFY_COMPILER)
    set(value) { put(NativeConfigurationKeys.VERIFY_COMPILER, value) }

var CompilerConfiguration.writeDependenciesOfProducedKlibTo: String?
    get() = get(NativeConfigurationKeys.WRITE_DEPENDENCIES_OF_PRODUCED_KLIB_TO)
    set(value) { put(NativeConfigurationKeys.WRITE_DEPENDENCIES_OF_PRODUCED_KLIB_TO, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.debugInfoVersion: Int?
    get() = get(NativeConfigurationKeys.DEBUG_INFO_VERSION)
    set(value) { put(NativeConfigurationKeys.DEBUG_INFO_VERSION, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.objcGenerics: Boolean
    get() = getBoolean(NativeConfigurationKeys.OBJC_GENERICS)
    set(value) { put(NativeConfigurationKeys.OBJC_GENERICS, value) }

var CompilerConfiguration.debugPrefixMap: Map<String, String>
    get() = getMap(NativeConfigurationKeys.DEBUG_PREFIX_MAP)
    set(value) { put(NativeConfigurationKeys.DEBUG_PREFIX_MAP, value) }

var CompilerConfiguration.preLinkCaches: Boolean
    get() = getBoolean(NativeConfigurationKeys.PRE_LINK_CACHES)
    set(value) { put(NativeConfigurationKeys.PRE_LINK_CACHES, value) }

var CompilerConfiguration.overrideKonanProperties: Map<String, String>
    get() = getMap(NativeConfigurationKeys.OVERRIDE_KONAN_PROPERTIES)
    set(value) { put(NativeConfigurationKeys.OVERRIDE_KONAN_PROPERTIES, value) }

var CompilerConfiguration.propertyLazyInitialization: Boolean
    get() = getBoolean(NativeConfigurationKeys.PROPERTY_LAZY_INITIALIZATION)
    set(value) { put(NativeConfigurationKeys.PROPERTY_LAZY_INITIALIZATION, value) }

var CompilerConfiguration.externalDependencies: String?
    get() = get(NativeConfigurationKeys.EXTERNAL_DEPENDENCIES)
    set(value) { put(NativeConfigurationKeys.EXTERNAL_DEPENDENCIES, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.llvmVariant: LlvmVariant?
    get() = get(NativeConfigurationKeys.LLVM_VARIANT)
    set(value) { put(NativeConfigurationKeys.LLVM_VARIANT, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.runtimeLogs: String?
    get() = get(NativeConfigurationKeys.RUNTIME_LOGS)
    set(value) { put(NativeConfigurationKeys.RUNTIME_LOGS, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.testDumpOutputPath: String?
    get() = get(NativeConfigurationKeys.TEST_DUMP_OUTPUT_PATH)
    set(value) { put(NativeConfigurationKeys.TEST_DUMP_OUTPUT_PATH, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.omitFrameworkBinary: Boolean
    get() = getBoolean(NativeConfigurationKeys.OMIT_FRAMEWORK_BINARY)
    set(value) { put(NativeConfigurationKeys.OMIT_FRAMEWORK_BINARY, value) }

var CompilerConfiguration.compileFromBitcode: String?
    get() = get(NativeConfigurationKeys.COMPILE_FROM_BITCODE)
    set(value) { put(NativeConfigurationKeys.COMPILE_FROM_BITCODE, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.serializedDependencies: String?
    get() = get(NativeConfigurationKeys.SERIALIZED_DEPENDENCIES)
    set(value) { put(NativeConfigurationKeys.SERIALIZED_DEPENDENCIES, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.saveDependenciesPath: String?
    get() = get(NativeConfigurationKeys.SAVE_DEPENDENCIES_PATH)
    set(value) { put(NativeConfigurationKeys.SAVE_DEPENDENCIES_PATH, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.saveLlvmIrDirectory: String?
    get() = get(NativeConfigurationKeys.SAVE_LLVM_IR_DIRECTORY)
    set(value) { put(NativeConfigurationKeys.SAVE_LLVM_IR_DIRECTORY, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.konanDataDir: String?
    get() = get(NativeConfigurationKeys.KONAN_DATA_DIR)
    set(value) { put(NativeConfigurationKeys.KONAN_DATA_DIR, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.manifestNativeTargets: List<KonanTarget>
    get() = getList(NativeConfigurationKeys.MANIFEST_NATIVE_TARGETS)
    set(value) { put(NativeConfigurationKeys.MANIFEST_NATIVE_TARGETS, value) }

var CompilerConfiguration.llvmModulePasses: String?
    get() = get(NativeConfigurationKeys.LLVM_MODULE_PASSES)
    set(value) { put(NativeConfigurationKeys.LLVM_MODULE_PASSES, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.llvmLtoPasses: String?
    get() = get(NativeConfigurationKeys.LLVM_LTO_PASSES)
    set(value) { put(NativeConfigurationKeys.LLVM_LTO_PASSES, requireNotNull(value) { "nullable values are not allowed" }) }

