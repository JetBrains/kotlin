/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.js.config

import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.incremental.js.IncrementalDataProvider
import org.jetbrains.kotlin.incremental.js.IncrementalNextRoundChecker
import org.jetbrains.kotlin.incremental.js.IncrementalResultsConsumer
import org.jetbrains.kotlin.konan.file.ZipFileSystemAccessor
import org.jetbrains.kotlin.serialization.js.ModuleKind
import java.io.File

object JSConfigurationKeys {
    @JvmField
    val TRANSITIVE_LIBRARIES: CompilerConfigurationKey<List<String?>> =
        CompilerConfigurationKey.create<List<String?>>("library files for transitive dependencies")

    @JvmField
    val LIBRARIES: CompilerConfigurationKey<List<String?>> =
        CompilerConfigurationKey.create<List<String?>>("library file paths")

    @JvmField
    val SOURCE_MAP: CompilerConfigurationKey<Boolean?> = CompilerConfigurationKey.create<Boolean>("generate source map")

    @JvmField
    val USE_DEBUGGER_CUSTOM_FORMATTERS: CompilerConfigurationKey<Boolean> =
        CompilerConfigurationKey.create<Boolean>("add import of debugger custom formatters")

    @JvmField
    val OUTPUT_DIR: CompilerConfigurationKey<File?> = CompilerConfigurationKey.create<File>("output directory")

    @JvmField
    val SOURCE_MAP_PREFIX: CompilerConfigurationKey<String> =
        CompilerConfigurationKey.create<String>("prefix to add to paths in source map")

    @JvmField
    val SOURCE_MAP_SOURCE_ROOTS: CompilerConfigurationKey<List<String>> =
        CompilerConfigurationKey.create<List<String>>("base directories used to calculate relative paths for source map")

    @JvmField
    val SOURCE_MAP_EMBED_SOURCES: CompilerConfigurationKey<SourceMapSourceEmbedding> =
        CompilerConfigurationKey.create<SourceMapSourceEmbedding>("embed source files into source map")

    @JvmField
    val SOURCEMAP_NAMES_POLICY: CompilerConfigurationKey<SourceMapNamesPolicy?> = CompilerConfigurationKey.create<SourceMapNamesPolicy>(
        "a policy to generate a mapping from generated identifiers to their corresponding original names"
    )

    @JvmField
    val SOURCE_MAP_INCLUDE_MAPPINGS_FROM_UNAVAILABLE_FILES: CompilerConfigurationKey<Boolean?> = CompilerConfigurationKey.create<Boolean>(
        "insert source mappings from libraries even if their sources are unavailable on the end-user machine"
    )

    @JvmField
    val META_INFO: CompilerConfigurationKey<Boolean?> = CompilerConfigurationKey.create<Boolean>("generate .meta.js and .kjsm files")

    @JvmField
    val TARGET: CompilerConfigurationKey<EcmaVersion?> = CompilerConfigurationKey.create<EcmaVersion>("ECMA version target")

    @JvmField
    val MODULE_KIND: CompilerConfigurationKey<ModuleKind?> = CompilerConfigurationKey.create<ModuleKind>("module kind")

    @JvmField
    val INCREMENTAL_DATA_PROVIDER: CompilerConfigurationKey<IncrementalDataProvider> =
        CompilerConfigurationKey.create<IncrementalDataProvider>("incremental data provider")

    @JvmField
    val INCREMENTAL_RESULTS_CONSUMER: CompilerConfigurationKey<IncrementalResultsConsumer> =
        CompilerConfigurationKey.create<IncrementalResultsConsumer>("incremental results consumer")

    @JvmField
    val INCREMENTAL_NEXT_ROUND_CHECKER: CompilerConfigurationKey<IncrementalNextRoundChecker> =
        CompilerConfigurationKey.create<IncrementalNextRoundChecker>("incremental compilation next round checker")

    @JvmField
    val FRIEND_PATHS_DISABLED: CompilerConfigurationKey<Boolean> =
        CompilerConfigurationKey.create<Boolean>("disable support for friend paths")

    @JvmField
    val FRIEND_PATHS: CompilerConfigurationKey<List<String?>> =
        CompilerConfigurationKey.create<List<String?>>("friend module paths")

    @JvmField
    val METADATA_ONLY: CompilerConfigurationKey<Boolean> =
        CompilerConfigurationKey.create<Boolean>("generate .meta.js and .kjsm files only")

    @JvmField
    val DEVELOPER_MODE: CompilerConfigurationKey<Boolean?> = CompilerConfigurationKey.create<Boolean>("enables additional checkers")

    @JvmField
    val GENERATE_COMMENTS_WITH_FILE_PATH: CompilerConfigurationKey<Boolean> =
        CompilerConfigurationKey.create<Boolean>("generate comments with file path at the start of each file block")

    @JvmField
    val GENERATE_POLYFILLS: CompilerConfigurationKey<Boolean> =
        CompilerConfigurationKey.create<Boolean>("generate polyfills for newest properties, methods and classes from ES6+")

    @JvmField
    val DEFINE_PLATFORM_MAIN_FUNCTION_ARGUMENTS: CompilerConfigurationKey<String> =
        CompilerConfigurationKey.create<String>("provide platform specific args as a parameter of the main function")

    @JvmField
    val GENERATE_DTS: CompilerConfigurationKey<Boolean?> = CompilerConfigurationKey.create<Boolean>("generate TypeScript definition file")

    @JvmField
    val COMPILE_SUSPEND_AS_JS_GENERATOR: CompilerConfigurationKey<Boolean> =
        CompilerConfigurationKey.create<Boolean>("force suspend functions compilation int JS generator functions")

    @JvmField
    val COMPILE_LAMBDAS_AS_ES6_ARROW_FUNCTIONS: CompilerConfigurationKey<Boolean> =
        CompilerConfigurationKey.create<Boolean>("lower Kotlin lambdas into arrow functions instead of anonymous functions")

    @JvmField
    val GENERATE_REGION_COMMENTS: CompilerConfigurationKey<Boolean?> = CompilerConfigurationKey.create<Boolean>(
        "generate special comments at the start and the end of each file block, " +
                "it allows to fold them and navigate to them in the IDEA"
    )

    @JvmField
    val FILE_PATHS_PREFIX_MAP: CompilerConfigurationKey<MutableMap<String?, String?>> =
        CompilerConfigurationKey.create<MutableMap<String?, String?>>(
            "this map used to shorten/replace prefix of paths in comments with file paths, " +
                    "including region comments"
        )

    @JvmField
    val PRINT_REACHABILITY_INFO: CompilerConfigurationKey<Boolean> =
        CompilerConfigurationKey.create<Boolean>("print declarations' reachability info during performing DCE")

    @JvmField
    val DUMP_REACHABILITY_INFO_TO_FILE: CompilerConfigurationKey<String> =
        CompilerConfigurationKey.create<String>("dump declarations' reachability info to file during performing DCE")

    @JvmField
    val FAKE_OVERRIDE_VALIDATOR: CompilerConfigurationKey<Boolean> =
        CompilerConfigurationKey.create<Boolean>("IR fake override validator")

    @JvmField
    val PROPERTY_LAZY_INITIALIZATION: CompilerConfigurationKey<Boolean> =
        CompilerConfigurationKey.create<Boolean>("perform lazy initialization for properties")

    @JvmField
    val GENERATE_INLINE_ANONYMOUS_FUNCTIONS: CompilerConfigurationKey<Boolean> =
        CompilerConfigurationKey.create<Boolean>("translate lambdas into in-line anonymous functions")

    @JvmField
    val GENERATE_STRICT_IMPLICIT_EXPORT: CompilerConfigurationKey<Boolean> =
        CompilerConfigurationKey.create<Boolean>("enable strict implicitly exported entities types inside d.ts files")

    @JvmField
    val ZIP_FILE_SYSTEM_ACCESSOR: CompilerConfigurationKey<ZipFileSystemAccessor> =
        CompilerConfigurationKey.create<ZipFileSystemAccessor>("zip file system accessor, used for klib reading")

    @JvmField
    val OPTIMIZE_GENERATED_JS: CompilerConfigurationKey<Boolean> =
        CompilerConfigurationKey.create<Boolean>("perform additional optimizations on the generated JS code")

    @JvmField
    val USE_ES6_CLASSES: CompilerConfigurationKey<Boolean?> = CompilerConfigurationKey.create<Boolean>("perform ES6 class usage")
}
