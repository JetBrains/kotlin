/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.configuration

import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.name.ClassId
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

    // Ordered ClassIds (1..N-1) of prior REPL snippets already compiled through the *regular*
    // pipeline (a plain `.repl.kts` source root file, `-Xallow-any-scripts-in-source-roots`, `-d`
    // output), whose compiled classes reach this compile purely via the regular classpath. Only
    // meaningful when [REPL_SNIPPET_REGULAR_MODE] is set; see its doc for the overall mechanism.
    val REPL_SNIPPET_PRIOR_CLASSES: CompilerConfigurationKey<List<ClassId>> =
        CompilerConfigurationKey.create("REPL_SNIPPET_PRIOR_CLASSES")

    // Enables compiling `.repl.kts` sources as chained REPL snippets on the *regular* JVM
    // frontend/backend pipeline (used together with `-Xallow-any-scripts-in-source-roots`): a
    // `.repl.kts` source is marked via `KtScript.markAsReplSnippet()` in
    // `ScriptingProcessSourcesBeforeCompilingExtension` and passed through unmodified; the FIR
    // REPL-snippet extensions are additionally registered, configured with a dedicated
    // `ScriptDefinition` (resultField-capturing `ScriptCompilationConfiguration` matching
    // `.repl.kts`) and a `ClasspathBackedFirReplHistoryProvider` built from
    // [REPL_SNIPPET_PRIOR_CLASSES]. This is the same-machine CLI/daemon caller's way of getting
    // chained-snippet compilation with no bespoke artifact format — prior snippets are just
    // classpath entries plus their ClassIds; see the on-daemon JSR-223 example
    // (`:examples:scripting-jsr223-daemon`)'s `DaemonReplCompiler`.
    val REPL_SNIPPET_REGULAR_MODE: CompilerConfigurationKey<Boolean> =
        CompilerConfigurationKey.create("REPL_SNIPPET_REGULAR_MODE")
}
