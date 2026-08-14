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
 * The default [ScriptCompilationConfiguration.resultField] name. A snippet's last-expression
 * value is emitted under this field unless a script definition overrides it.
 */
const val DEFAULT_SNIPPET_RESULT_FIELD_NAME = "\$\$result"

private const val SNIPPET_RESULT_FIELD_TYPE_NAME = "kotlin.Any"

/**
 * Wraps an already-compiled REPL snippet's output classes into a [KJvmCompiledScript], loading
 * them through a plain [KJvmCompiledModuleFromClassPath] classloader over [classPath]. This goes
 * through the same cross-snippet classloader chaining as [getOrCreateActualClassloader].
 *
 * Intended for an out-of-process snippet compiler that compiles straight to an output directory
 * and only needs to name the resulting wrapper class ([snippetClassFQName]).
 *
 * Pass `resultFieldName = null` for a snippet with no result field.
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
