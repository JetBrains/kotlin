/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.api

class CodeSnippet(val code: SourceCode, val id: String? = null)

interface CompiledSnippet {

    val previous: CompiledSnippet?

    val snippetId: String?

    val configuration: ScriptCompilationConfiguration
}

interface ReplCompiler {

    // TODO: replace with special result returned from 'invoke'
    fun isComplete(snippets: CodeSnippet, configuration: ScriptCompilationConfiguration): ResultWithDiagnostics<Boolean>

    operator fun invoke(
        snippets: Iterable<CodeSnippet>,
        configuration: ScriptCompilationConfiguration,
        lastCompiledSnippet: CompiledSnippet?
    )
            : ResultWithDiagnostics<CompiledSnippet>
}

interface EvaluatedSnippet {

    val previous: EvaluatedSnippet?

    val result: Any?

    val hasResult: Boolean

    val snippetId: String?

    val configuration: ScriptEvaluationConfiguration
}

interface ReplEvaluator {

    val lastEvaluatedSnippet: EvaluatedSnippet?

    // asserts that snippet sequence is valid
    operator fun invoke(snippet: CompiledSnippet, configuration: ScriptEvaluationConfiguration)
            : ResultWithDiagnostics<EvaluatedSnippet>
}