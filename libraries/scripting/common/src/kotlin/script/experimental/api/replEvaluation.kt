/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.api

import kotlin.script.experimental.util.LinkedSnippet

/**
 * Evaluated snippet type, the most common return type for
 * [ReplEvaluator.eval] and [ReplEvaluator.lastEvaluatedSnippet], boxed into
 * [LinkedSnippet] container
 */
interface EvaluatedSnippet {
    /**
     * Link to the compiled snippet used for the evaluation
     */
    val compiledSnippet: CompiledSnippet

    /**
     * Real evaluation configuration for this snippet
     */
    val configuration: ScriptEvaluationConfiguration

    /**
     * Result of the script evaluation.
     */
    val result: ResultValue
}

/**
 * REPL Evaluator interface which is used for compiled snippets evaluation
 * @param CompiledSnippetT Should be equal to or wider than the corresponding type parameter of compiler.
 *                         Lets evaluator use specific versions of compiled script without type conversion.
 * @param EvaluatedSnippetT Implementation of [EvaluatedSnippet] which is returned by this evaluator
 */
interface ReplEvaluator<CompiledSnippetT : CompiledSnippet, EvaluatedSnippetT : EvaluatedSnippet> {

    /**
     * Reference to the last evaluation result
     */
    val lastEvaluatedSnippet: LinkedSnippet<EvaluatedSnippetT>?

    /**
     * Evaluates compiled snippet and returns result for it.
     * Should assert that snippet sequence is valid.
     * @param snippet Snippet to evaluate.
     * @param configuration Evaluation configuration used.
     * @return Evaluation result
     */
    suspend fun eval(
        snippet: LinkedSnippet<out CompiledSnippetT>,
        configuration: ScriptEvaluationConfiguration
    ): ResultWithDiagnostics<LinkedSnippet<EvaluatedSnippetT>>
}