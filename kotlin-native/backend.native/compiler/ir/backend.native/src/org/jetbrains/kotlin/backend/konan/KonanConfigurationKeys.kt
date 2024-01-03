/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.konan.target.CompilerOutputKind

class KonanConfigKeys {
    companion object {
        // Keep the list lexically sorted.
        val BUNDLE_ID: CompilerConfigurationKey<String>
                = CompilerConfigurationKey.create("bundle ID to be set in Info.plist of a produced framework")
        val CHECK_DEPENDENCIES: CompilerConfigurationKey<Boolean>
                = CompilerConfigurationKey.create("check dependencies and download the missing ones")
        val DEBUG: CompilerConfigurationKey<Boolean>
                = CompilerConfigurationKey.create("add debug information")
        val FAKE_OVERRIDE_VALIDATOR: CompilerConfigurationKey<Boolean>
                = CompilerConfigurationKey.create("fake override validator")
        val BITCODE_EMBEDDING_MODE: CompilerConfigurationKey<BitcodeEmbedding.Mode>
                = CompilerConfigurationKey.create("bitcode embedding mode")
        val EMIT_LAZY_OBJC_HEADER_FILE: CompilerConfigurationKey<String?> =
                CompilerConfigurationKey.create("output file to emit lazy Obj-C header")
        val ENABLE_ASSERTIONS: CompilerConfigurationKey<Boolean>
                = CompilerConfigurationKey.create("enable runtime assertions in generated code")
        val ENTRY: CompilerConfigurationKey<String?>
                = CompilerConfigurationKey.create("fully qualified main() name")
        val EXPORTED_LIBRARIES: CompilerConfigurationKey<List<String>>
                = CompilerConfigurationKey.create<List<String>>("libraries included into produced framework API")
        val FULL_EXPORTED_NAME_PREFIX: CompilerConfigurationKey<String?>
                = CompilerConfigurationKey.create("prefix used when exporting Kotlin names to other languages")
        val LIBRARY_TO_ADD_TO_CACHE: CompilerConfigurationKey<String?>
                = CompilerConfigurationKey.create<String?>("path to library that to be added to cache")
        val CACHE_DIRECTORIES: CompilerConfigurationKey<List<String>>
                = CompilerConfigurationKey.create<List<String>>("paths to directories containing caches")
        val AUTO_CACHEABLE_FROM: CompilerConfigurationKey<List<String>>
                = CompilerConfigurationKey.create<List<String>>("paths to the root directories from which dependencies are to be cached automatically")
        val AUTO_CACHE_DIR: CompilerConfigurationKey<String>
                = CompilerConfigurationKey.create<String>("path to the directory where to put caches for auto-cacheable dependencies")
        val INCREMENTAL_CACHE_DIR: CompilerConfigurationKey<String>
                = CompilerConfigurationKey.create<String>("path to the directory where to put incremental build caches")
        val CACHED_LIBRARIES: CompilerConfigurationKey<Map<String, String>>
                = CompilerConfigurationKey.create<Map<String, String>>("mapping from library paths to cache paths")
        val FILES_TO_CACHE: CompilerConfigurationKey<List<String>>
                = CompilerConfigurationKey.create<List<String>>("which files should be compiled to cache")
        val MAKE_PER_FILE_CACHE: CompilerConfigurationKey<Boolean>
                = CompilerConfigurationKey.create<Boolean>("make per-file cache")
        val FRAMEWORK_IMPORT_HEADERS: CompilerConfigurationKey<List<String>>
                = CompilerConfigurationKey.create<List<String>>("headers imported to framework header")
        val FRIEND_MODULES: CompilerConfigurationKey<List<String>>
                = CompilerConfigurationKey.create<List<String>>("friend module paths")
        val REFINES_MODULES: CompilerConfigurationKey<List<String>>
                = CompilerConfigurationKey.create<List<String>>("refines module paths")
        val GENERATE_TEST_RUNNER: CompilerConfigurationKey<TestRunnerKind>
                = CompilerConfigurationKey.create("generate test runner") 
        val INCLUDED_BINARY_FILES: CompilerConfigurationKey<List<String>>
                = CompilerConfigurationKey.create("included binary file paths")
        val KONAN_HOME: CompilerConfigurationKey<String>
                = CompilerConfigurationKey.create("overridden compiler distribution path")
        val LIBRARY_FILES: CompilerConfigurationKey<List<String>> 
                = CompilerConfigurationKey.create("library file paths")
        val LIBRARY_VERSION: CompilerConfigurationKey<String?>
                = CompilerConfigurationKey.create("library version")
        val LIGHT_DEBUG: CompilerConfigurationKey<Boolean?>
                = CompilerConfigurationKey.create("add light debug information")
        val GENERATE_DEBUG_TRAMPOLINE: CompilerConfigurationKey<Boolean?>
                = CompilerConfigurationKey.create("generates debug trampolines to make debugger breakpoint resolution more accurate")
        val LINKER_ARGS: CompilerConfigurationKey<List<String>>
                = CompilerConfigurationKey.create("additional linker arguments")
        val LIST_TARGETS: CompilerConfigurationKey<Boolean>
                = CompilerConfigurationKey.create("list available targets")
        val MANIFEST_FILE: CompilerConfigurationKey<String?> 
                = CompilerConfigurationKey.create("provide manifest addend file")
        val HEADER_KLIB: CompilerConfigurationKey<String?>
                = CompilerConfigurationKey.create("path to file where header klib should be produced")
        val MODULE_NAME: CompilerConfigurationKey<String?>
                = CompilerConfigurationKey.create("module name")
        val NATIVE_LIBRARY_FILES: CompilerConfigurationKey<List<String>>
                = CompilerConfigurationKey.create("native library file paths")
        val NODEFAULTLIBS: CompilerConfigurationKey<Boolean>
                = CompilerConfigurationKey.create("don't link with the default libraries")
        val NOENDORSEDLIBS: CompilerConfigurationKey<Boolean>
                = CompilerConfigurationKey.create("don't link with the endorsed libraries")
        val NOMAIN: CompilerConfigurationKey<Boolean> 
                = CompilerConfigurationKey.create("assume 'main' entry point to be provided by external libraries")
        val NOSTDLIB: CompilerConfigurationKey<Boolean> 
                = CompilerConfigurationKey.create("don't link with stdlib")
        val NOPACK: CompilerConfigurationKey<Boolean> 
                = CompilerConfigurationKey.create("don't the library into a klib file")
        val OPTIMIZATION: CompilerConfigurationKey<Boolean> 
                = CompilerConfigurationKey.create("optimized compilation")
        val OUTPUT: CompilerConfigurationKey<String> 
                = CompilerConfigurationKey.create("program or library name")
        val OVERRIDE_CLANG_OPTIONS: CompilerConfigurationKey<List<String>>
                = CompilerConfigurationKey.create("arguments for clang")
        val ALLOCATION_MODE: CompilerConfigurationKey<AllocationMode>
                = CompilerConfigurationKey.create("allocation mode")
        val EXPORT_KDOC: CompilerConfigurationKey<Boolean>
                = CompilerConfigurationKey.create("export KDoc into klib and framework")
        val PRINT_BITCODE: CompilerConfigurationKey<Boolean>
                = CompilerConfigurationKey.create("print bitcode")
        val PRINT_IR: CompilerConfigurationKey<Boolean>
                = CompilerConfigurationKey.create("print ir")
        val PRINT_FILES: CompilerConfigurationKey<Boolean>
                = CompilerConfigurationKey.create("print files")
        val PRODUCE: CompilerConfigurationKey<CompilerOutputKind>
                = CompilerConfigurationKey.create("compiler output kind")
        val PURGE_USER_LIBS: CompilerConfigurationKey<Boolean>
                = CompilerConfigurationKey.create("purge user-specified libs too")
        val REPOSITORIES: CompilerConfigurationKey<List<String>>
                = CompilerConfigurationKey.create("library search path repositories")
        val RUNTIME_FILE: CompilerConfigurationKey<String?> 
                = CompilerConfigurationKey.create("override default runtime file path")
        val INCLUDED_LIBRARIES: CompilerConfigurationKey<List<String>>
                = CompilerConfigurationKey("klibs processed in the same manner as source files")
        val SHORT_MODULE_NAME: CompilerConfigurationKey<String?>
                = CompilerConfigurationKey("short module name for IDE and export")
        val STATIC_FRAMEWORK: CompilerConfigurationKey<Boolean>
                = CompilerConfigurationKey.create("produce a static library for a framework")
        val TARGET: CompilerConfigurationKey<String?>
                = CompilerConfigurationKey.create("target we compile for")
        val TEMPORARY_FILES_DIR: CompilerConfigurationKey<String?>
                = CompilerConfigurationKey.create("directory for temporary files")
        val SAVE_LLVM_IR: CompilerConfigurationKey<List<String>>
                = CompilerConfigurationKey.create("save LLVM IR")
        val VERIFY_BITCODE: CompilerConfigurationKey<Boolean>
                = CompilerConfigurationKey.create("verify bitcode")
        val VERIFY_IR: CompilerConfigurationKey<IrVerificationMode>
                = CompilerConfigurationKey.create("IR verification mode")
        val VERIFY_COMPILER: CompilerConfigurationKey<Boolean>
                = CompilerConfigurationKey.create("verify compiler")
        val DEBUG_INFO_VERSION: CompilerConfigurationKey<Int>
                = CompilerConfigurationKey.create("debug info format version")
        val OBJC_GENERICS: CompilerConfigurationKey<Boolean>
                = CompilerConfigurationKey.create("write objc header with generics support")
        val DEBUG_PREFIX_MAP: CompilerConfigurationKey<Map<String, String>>
                = CompilerConfigurationKey.create("remap file source paths in debug info")
        val PRE_LINK_CACHES: CompilerConfigurationKey<Boolean>
                = CompilerConfigurationKey.create("perform compiler caches pre-link")
        val OVERRIDE_KONAN_PROPERTIES: CompilerConfigurationKey<Map<String, String>>
                = CompilerConfigurationKey.create("override konan.properties values")
        val DESTROY_RUNTIME_MODE: CompilerConfigurationKey<DestroyRuntimeMode>
                = CompilerConfigurationKey.create("when to destroy runtime")
        val PROPERTY_LAZY_INITIALIZATION: CompilerConfigurationKey<Boolean>
                = CompilerConfigurationKey.create("lazy top level properties initialization")
        val WORKER_EXCEPTION_HANDLING: CompilerConfigurationKey<WorkerExceptionHandling> = CompilerConfigurationKey.create("unhandled exception processing in Worker.executeAfter")
        val EXTERNAL_DEPENDENCIES: CompilerConfigurationKey<String?> =
                CompilerConfigurationKey.create("use external dependencies to enhance IR linker error messages")
        val LLVM_VARIANT: CompilerConfigurationKey<LlvmVariant?> = CompilerConfigurationKey.create("llvm variant")
        val RUNTIME_LOGS: CompilerConfigurationKey<String> = CompilerConfigurationKey.create("enable runtime logging")
        val LAZY_IR_FOR_CACHES: CompilerConfigurationKey<Boolean> = CompilerConfigurationKey.create("use lazy IR for cached libraries")
        val TEST_DUMP_OUTPUT_PATH: CompilerConfigurationKey<String?> = CompilerConfigurationKey.create("path to a file to dump the list of all available tests")
        val OMIT_FRAMEWORK_BINARY: CompilerConfigurationKey<Boolean> = CompilerConfigurationKey.create("do not generate binary in framework")
        val COMPILE_FROM_BITCODE: CompilerConfigurationKey<String?> = CompilerConfigurationKey.create("path to bitcode file to compile")
        val SERIALIZED_DEPENDENCIES: CompilerConfigurationKey<String?> = CompilerConfigurationKey.create("path to serialized dependencies for native linking")
        val SAVE_DEPENDENCIES_PATH: CompilerConfigurationKey<String?> = CompilerConfigurationKey.create("path to save serialized dependencies to")
        val SAVE_LLVM_IR_DIRECTORY: CompilerConfigurationKey<String?> = CompilerConfigurationKey.create("directory to store LLVM IR from phases")
        val KONAN_DATA_DIR: CompilerConfigurationKey<String?> = CompilerConfigurationKey.create("directory for storing konan dependencies, cache and prebuilds")
    }
}

