/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.js.config;

import org.jetbrains.kotlin.config.CompilerConfigurationKey;
import org.jetbrains.kotlin.incremental.js.IncrementalNextRoundChecker;
import org.jetbrains.kotlin.incremental.js.IncrementalDataProvider;
import org.jetbrains.kotlin.incremental.js.IncrementalResultsConsumer;
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

    public static final CompilerConfigurationKey<Boolean> META_INFO =
            CompilerConfigurationKey.create("generate .meta.js and .kjsm files");

    public static final CompilerConfigurationKey<EcmaVersion> TARGET =
            CompilerConfigurationKey.create("ECMA version target");

    public static final CompilerConfigurationKey<ModuleKind> MODULE_KIND =
            CompilerConfigurationKey.create("module kind");

    public static final CompilerConfigurationKey<Boolean> TYPED_ARRAYS_ENABLED =
            CompilerConfigurationKey.create("TypedArrays enabled");

    public static final CompilerConfigurationKey<IncrementalDataProvider> INCREMENTAL_DATA_PROVIDER =
            CompilerConfigurationKey.create("incremental data provider");

    public static final CompilerConfigurationKey<IncrementalResultsConsumer> INCREMENTAL_RESULTS_CONSUMER =
            CompilerConfigurationKey.create("incremental results consumer");

    public static final CompilerConfigurationKey<IncrementalNextRoundChecker> INCREMENTAL_NEXT_ROUND_CHECKER =
            CompilerConfigurationKey.create("incremental compilation next round checker");

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

    public static final CompilerConfigurationKey<Boolean> GENERATE_REGION_COMMENTS =
            CompilerConfigurationKey.create("generate special comments at the start and the end of each file block, " +
                                            "it allows to fold them and navigate to them in the IDEA");

    public static final CompilerConfigurationKey<Map<String, String>> FILE_PATHS_PREFIX_MAP =
            CompilerConfigurationKey.create("this map used to shorten/replace prefix of paths in comments with file paths, " +
                                            "including region comments");
}
