/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.configuration

import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import java.io.File

object ScriptingConfigurationKeys {
    val SCRIPT_DEFINITIONS = CompilerConfigurationKey.create<List<ScriptDefinition>>("SCRIPT_DEFINITIONS")

    @Suppress("DEPRECATION") //KT-82551
    val SCRIPT_DEFINITIONS_SOURCES =
        CompilerConfigurationKey.create<List<org.jetbrains.kotlin.scripting.definitions.ScriptDefinitionsSource>>("SCRIPT_DEFINITIONS_SOURCES")

    val DISABLE_SCRIPTING_PLUGIN_OPTION: CompilerConfigurationKey<Boolean> =
        CompilerConfigurationKey.create("DISABLE_SCRIPTING_PLUGIN_OPTION")

    val SCRIPT_DEFINITIONS_CLASSES: CompilerConfigurationKey<List<String>> =
        CompilerConfigurationKey.create("SCRIPT_DEFINITIONS_CLASSES")

    val SCRIPT_DEFINITIONS_CLASSPATH: CompilerConfigurationKey<List<File>> =
        CompilerConfigurationKey.create("SCRIPT_DEFINITIONS_CLASSPATH")

    val DISABLE_SCRIPT_DEFINITIONS_FROM_CLASSPATH_OPTION: CompilerConfigurationKey<Boolean> =
        CompilerConfigurationKey.create("DISABLE_SCRIPT_DEFINITIONS_FROM_CLASSPATH_OPTION")

    // Do not automatically load compiler-supplied script definitions, like main-kts.
    val DISABLE_SCRIPT_DEFINITIONS_AUTOLOADING_OPTION: CompilerConfigurationKey<Boolean> =
        CompilerConfigurationKey.create("DISABLE_SCRIPT_DEFINITIONS_AUTOLOADING_OPTION")

    val LEGACY_SCRIPT_RESOLVER_ENVIRONMENT_OPTION: CompilerConfigurationKey<MutableMap<String, Any?>> =
        CompilerConfigurationKey.create("LEGACY_SCRIPT_RESOLVER_ENVIRONMENT_OPTION")

    // Enable additional IR generation which contains script expressions evaluation info.
    val ENABLE_SCRIPT_EXPLANATION_OPTION: CompilerConfigurationKey<Boolean> =
        CompilerConfigurationKey.create("ENABLE_SCRIPT_EXPLANATION_OPTION")

    // Do not attempt to use script compilation cache, even if provided by the definition
    val DISABLE_SCRIPT_COMPILATION_CACHE: CompilerConfigurationKey<Boolean> =
        CompilerConfigurationKey.create("DISABLE_SCRIPT_COMPILATION_CACHE")

    // Stateless K2 REPL snippet compilation (migration step 3, Q5d). When set, the regular
    // compilation path compiles the single input source as a REPL snippet against the prior-snippet
    // artifacts in [REPL_SNIPPET_PRIOR_ARTIFACTS] (in order) and writes the produced artifact to
    // [REPL_SNIPPET_ARTIFACT_OUTPUT]. This is the parameter surface that lets snippet compilation be
    // driven both from the CLI and from a regular `CompileService.compile(...)` call, without any
    // REPL-specific transport (see plugins/scripting/.ai/target/90-open-questions.md Q5d).
    val REPL_SNIPPET_COMPILATION_MODE: CompilerConfigurationKey<Boolean> =
        CompilerConfigurationKey.create("REPL_SNIPPET_COMPILATION_MODE")

    // Ordered list of prior-snippet artifact files (1..N-1) feeding the snippet compilation state.
    // Each file is a `SnippetArtifactCodec`-encoded blob; order is significant.
    val REPL_SNIPPET_PRIOR_ARTIFACTS: CompilerConfigurationKey<List<File>> =
        CompilerConfigurationKey.create("REPL_SNIPPET_PRIOR_ARTIFACTS")

    // Destination file for the `SnippetArtifactCodec`-encoded artifact produced by a snippet compile.
    val REPL_SNIPPET_ARTIFACT_OUTPUT: CompilerConfigurationKey<File> =
        CompilerConfigurationKey.create("REPL_SNIPPET_ARTIFACT_OUTPUT")

    // Optional explicit name for the snippet being compiled in [REPL_SNIPPET_COMPILATION_MODE].
    // Stateless reconstruction keys snippets by their source name, so a multi-snippet sequence needs
    // distinct names. When the source enters through `-expression` (always synthetically named
    // `script.kts`), this key lets the caller assign a distinct, deterministic name per snippet —
    // which is what makes daemon/CLI-driven multi-snippet sequences resolve their priors correctly.
    val REPL_SNIPPET_NAME: CompilerConfigurationKey<String> =
        CompilerConfigurationKey.create("REPL_SNIPPET_NAME")
}
