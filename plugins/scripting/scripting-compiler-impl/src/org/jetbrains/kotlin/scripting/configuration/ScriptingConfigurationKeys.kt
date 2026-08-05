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

    // Ordered ClassIds of previous REPL snippets already compiled through the regular pipeline.
    // Their compiled classes must already be on this compile's classpath. Only meaningful when
    // [REPL_SNIPPET_REGULAR_MODE] is set.
    val REPL_SNIPPET_PRIOR_CLASSES: CompilerConfigurationKey<List<ClassId>> =
        CompilerConfigurationKey.create("REPL_SNIPPET_PRIOR_CLASSES")

    // Enables compiling `.repl.kts` sources as chained REPL snippets on the regular JVM
    // frontend/backend pipeline (used together with `-Xallow-any-scripts-in-source-roots`).
    // Sources are marked via `KtScript.markAsReplSnippet()` and passed through unmodified; the FIR
    // REPL-snippet extensions are registered with a `ClasspathBackedFirReplHistoryProvider` built
    // from [REPL_SNIPPET_PRIOR_CLASSES].
    val REPL_SNIPPET_REGULAR_MODE: CompilerConfigurationKey<Boolean> =
        CompilerConfigurationKey.create("REPL_SNIPPET_REGULAR_MODE")

    // Fully qualified names of extra implicit receiver types every `.repl.kts` snippet compiled in
    // [REPL_SNIPPET_REGULAR_MODE] should declare, in outer-to-inner scope order. Folded into the
    // dedicated `.repl.kts` `ScriptDefinition`'s `implicitReceivers(...)`. Needed because a
    // client's own `ScriptCompilationConfiguration` is process-local and does not cross the
    // client/daemon boundary; the receiver instances are still supplied client-side at evaluation
    // time.
    val REPL_SNIPPET_IMPLICIT_RECEIVERS: CompilerConfigurationKey<List<String>> =
        CompilerConfigurationKey.create("REPL_SNIPPET_IMPLICIT_RECEIVERS")

    // The base file-extension component that, combined with the fixed `"repl."` prefix, forms the
    // `.repl.<extension>` suffix used to recognize a [REPL_SNIPPET_REGULAR_MODE] REPL-snippet
    // source. Absent (default) means plain `"kts"`, giving `.repl.kts`. A client wired to a real
    // script definition (for example `MainKtsScript` with `fileExtension = "main.kts"`) passes that
    // value here so its `.repl.main.kts`-named sources are recognized too.
    val REPL_SNIPPET_FILE_EXTENSION: CompilerConfigurationKey<String> =
        CompilerConfigurationKey.create("REPL_SNIPPET_FILE_EXTENSION")
}
