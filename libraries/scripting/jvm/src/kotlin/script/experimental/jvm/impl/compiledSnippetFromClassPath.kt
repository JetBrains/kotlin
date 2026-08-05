/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvm.impl

import java.io.File
import kotlin.script.experimental.api.KotlinType
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.SourceCode

/**
 * The [ScriptCompilationConfiguration.resultField] default (see `kotlin.script.experimental.api.resultField`)
 * -- so this is the field name a snippet's last-expression value is emitted under unless a script
 * definition overrides it.
 */
const val DEFAULT_SNIPPET_RESULT_FIELD_NAME = "\$\$result"

private const val SNIPPET_RESULT_FIELD_TYPE_NAME = "kotlin.Any"

/**
 * Wraps an already-compiled REPL snippet's plain output classes into a [KJvmCompiledScript] a REPL
 * evaluator can run: the classes are loaded through a plain [KJvmCompiledModuleFromClassPath]
 * classloader over [classPath] (regular `.class` files or jars -- no bespoke artifact
 * deserialization involved), so the result is indistinguishable from an in-process compilation
 * result, including the cross-snippet classloader chaining
 * [getOrCreateActualClassloader] performs.
 *
 * Intended for an out-of-process (or otherwise "stateless") snippet compiler, which compiles a
 * snippet straight to an output directory and then only needs to name the resulting wrapper class
 * ([snippetClassFQName]) -- e.g. `kotlin.script.experimental.jvmhost.jsr223.daemon.DaemonReplCompiler`,
 * which predicts it from the source file name it wrote.
 *
 * [resultFieldName] is the name the compiled bytecode emits the snippet's last-expression value
 * under -- [DEFAULT_SNIPPET_RESULT_FIELD_NAME] unless the definition the snippet was actually
 * compiled with overrides [ScriptCompilationConfiguration.resultField]; `null` for a snippet
 * compiled without a result field at all.
 */
fun compiledSnippetFromClassPath(
    classPath: List<File>,
    snippetClassFQName: String,
    snippet: SourceCode,
    compilationConfiguration: ScriptCompilationConfiguration,
    resultFieldName: String? = DEFAULT_SNIPPET_RESULT_FIELD_NAME,
): KJvmCompiledScript =
    KJvmCompiledScript(
        sourceLocationId = snippet.locationId,
        compilationConfiguration = compilationConfiguration,
        scriptClassFQName = snippetClassFQName,
        resultField = resultFieldName?.let { it to KotlinType(SNIPPET_RESULT_FIELD_TYPE_NAME) },
        otherScripts = emptyList(),
        compiledModule = KJvmCompiledModuleFromClassPath(classPath),
    )
