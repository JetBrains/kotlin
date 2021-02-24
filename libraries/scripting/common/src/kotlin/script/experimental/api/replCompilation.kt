/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.api

import kotlin.script.experimental.util.LinkedSnippet

/**
 * Compiled snippet type, the most common return type for
 * [ReplCompiler.compile] and [ReplCompiler.lastCompiledSnippet], boxed into
 * [LinkedSnippet] container
 */
typealias CompiledSnippet = CompiledScript

/**
 * REPL Compiler interface which is used for compiling snippets
 * @param CompiledSnippetT Implementation of [CompiledSnippet] which is returned by this compiler
 */
interface ReplCompiler<CompiledSnippetT : CompiledSnippet> {
    /**
     * Reference to the last compile result
     */
    val lastCompiledSnippet: LinkedSnippet<CompiledSnippetT>?

    /**
     * Compiles snippet chain and returns compilation result for the *last* snippet in the chain.
     * Generally <b>changes</b> the internal state of implementing object.
     * @param snippets Chain of snippets to compile
     * @param configuration Compilation configuration which is used
     * @return Compilation result with the last compiled snippet in chain
     */
    suspend fun compile(
        snippets: Iterable<SourceCode>,
        configuration: ScriptCompilationConfiguration
    ): ResultWithDiagnostics<LinkedSnippet<CompiledSnippetT>>

    /**
     * Compiles snippet and returns compilation result for it.
     * Generally <b>changes</b> the internal state of implementing object.
     * @param snippet Snippet to compile
     * @param configuration Compilation configuration which is used
     * @return Compilation result
     */
    suspend fun compile(
        snippet: SourceCode,
        configuration: ScriptCompilationConfiguration
    ): ResultWithDiagnostics<LinkedSnippet<CompiledSnippetT>> = compile(listOf(snippet), configuration)
}

