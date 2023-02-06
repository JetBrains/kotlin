/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.config;

import org.jetbrains.kotlin.config.CompilerConfigurationKey;
import org.jetbrains.kotlin.serialization.js.ModuleKind;

import java.io.File;
import java.util.List;
import java.util.Map;

public class JSConfigurationKeys {
    public static final CompilerConfigurationKey<List<String>> TRANSITIVE_LIBRARIES =
            CompilerConfigurationKey.create("library files for transitive dependencies");

    public static final CompilerConfigurationKey<List<String>> LIBRARIES =
            CompilerConfigurationKey.create("library file paths");

    public static final CompilerConfigurationKey<Boolean> SOURCE_MAP =
            CompilerConfigurationKey.create("generate source map");

    public static final CompilerConfigurationKey<File> OUTPUT_DIR =
            CompilerConfigurationKey.create("output directory");

    public static final CompilerConfigurationKey<String> SOURCE_MAP_PREFIX =
            CompilerConfigurationKey.create("prefix to add to paths in source map");

    public static final CompilerConfigurationKey<List<String>> SOURCE_MAP_SOURCE_ROOTS =
            CompilerConfigurationKey.create("base directories used to calculate relative paths for source map");

    public static final CompilerConfigurationKey<SourceMapSourceEmbedding> SOURCE_MAP_EMBED_SOURCES =
            CompilerConfigurationKey.create("embed source files into source map");

    public static final CompilerConfigurationKey<SourceMapNamesPolicy> SOURCEMAP_NAMES_POLICY = CompilerConfigurationKey.create(
            "a policy to generate a mapping from generated identifiers to their corresponding original names");

    public static final CompilerConfigurationKey<Boolean> META_INFO =
            CompilerConfigurationKey.create("generate .meta.js and .kjsm files");

    public static final CompilerConfigurationKey<EcmaVersion> TARGET =
            CompilerConfigurationKey.create("ECMA version target");

    public static final CompilerConfigurationKey<ModuleKind> MODULE_KIND =
            CompilerConfigurationKey.create("module kind");

    public static final CompilerConfigurationKey<Boolean> TYPED_ARRAYS_ENABLED =
            CompilerConfigurationKey.create("TypedArrays enabled");

    public static final CompilerConfigurationKey<Boolean> FRIEND_PATHS_DISABLED =
            CompilerConfigurationKey.create("disable support for friend paths");

    public static final CompilerConfigurationKey<List<String>> FRIEND_PATHS =
            CompilerConfigurationKey.create("friend module paths");

    public static final CompilerConfigurationKey<Boolean> METADATA_ONLY =
            CompilerConfigurationKey.create("generate .meta.js and .kjsm files only");

    public static final CompilerConfigurationKey<Boolean> DEVELOPER_MODE =
            CompilerConfigurationKey.create("enables additional checkers");

    public static final CompilerConfigurationKey<Boolean> GENERATE_COMMENTS_WITH_FILE_PATH =
            CompilerConfigurationKey.create("generate comments with file path at the start of each file block");

    public static final CompilerConfigurationKey<Boolean> GENERATE_POLYFILLS =
            CompilerConfigurationKey.create("generate polyfills for newest properties, methods and classes from ES6+");

    public static final CompilerConfigurationKey<Boolean> GENERATE_DTS =
            CompilerConfigurationKey.create("generate TypeScript definition file");

    public static final CompilerConfigurationKey<Boolean> GENERATE_REGION_COMMENTS =
            CompilerConfigurationKey.create("generate special comments at the start and the end of each file block, " +
                                            "it allows to fold them and navigate to them in the IDEA");

    public static final CompilerConfigurationKey<Map<String, String>> FILE_PATHS_PREFIX_MAP =
            CompilerConfigurationKey.create("this map used to shorten/replace prefix of paths in comments with file paths, " +
                                            "including region comments");

    public static final CompilerConfigurationKey<Boolean> GENERATE_INLINE_ANONYMOUS_FUNCTIONS =
            CompilerConfigurationKey.create("translate lambdas into in-line anonymous functions");

    public static final CompilerConfigurationKey<Boolean> GENERATE_STRICT_IMPLICIT_EXPORT =
            CompilerConfigurationKey.create("enable strict implicitly exported entities types inside d.ts files");

    public static final CompilerConfigurationKey<Boolean> WASM_ENABLE_ARRAY_RANGE_CHECKS =
            CompilerConfigurationKey.create("enable array range checks");

    public static final CompilerConfigurationKey<Boolean> WASM_ENABLE_ASSERTS =
            CompilerConfigurationKey.create("enable asserts");

    public static final CompilerConfigurationKey<Boolean> WASM_GENERATE_WAT =
            CompilerConfigurationKey.create("generate wat file");
}
